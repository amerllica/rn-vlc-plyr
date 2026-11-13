import NitroModules
import UIKit

final class PlaybackController {
  private static let snapshotQueue = DispatchQueue(label: "rn-vlc-plyr.snapshot")
  private static let startedStatuses: Set<PlayerStatus> = [.buffering, .playing, .paused]

  private let store: PlayerStore
  private var machine = PlaybackStateMachine()
  private var tracksGate = ChangeGate(initial: TracksSnapshot())
  private var videoSizeGate = VideoSizeGate()
  private var throttle = TimeUpdateThrottle()
  private var audioSession = PlaybackAudioSession()
  private let snapshots = SnapshotRequests()
  private var engineInstance: VlcEngine?
  private var vlcInstance: VlcInstance?
  private var currentRequest: MediaRequest?
  private var addedSlaves: [PlaybackSlave] = []
  private var reopen = ReopenState()
  private var resumeGuard = ResumeGuard()
  private var bufferingGate = ChangeGate<Int>()
  private var isShutDown = false
  private lazy var surface = VideoSurface()
  private lazy var session = MediaSessionCoordinator(store: store, control: self)

  init(store: PlayerStore) {
    self.store = store
  }

  private var isActive: Bool {
    !isShutDown && !store.isReleased
  }

  private var engine: VlcEngine {
    if let engineInstance { return engineInstance }
    let created = VlcEngine(drawable: surface.container, instance: currentVlcInstance())
    created.delegate = self
    engineInstance = created
    surface.engine = created
    return created
  }

  private func currentVlcInstance() -> VlcInstance {
    let settings = store.settings
    let options = VlcContract.instanceOptions(libvlcOptions: settings.libvlcOptions, subtitleStyle: settings.subtitleStyle)
    if let vlcInstance, vlcInstance.options == options { return vlcInstance }
    let created = VlcInstance(options: options)
    vlcInstance = created
    return created
  }

  func load(_ source: VlcSource?) {
    guard isActive else { return }
    defer { session.sourceDidChange() }
    resetMediaState()
    addedSlaves.removeAll()
    guard let source else {
      currentRequest = nil
      retireEngine()
      audioSession.deactivate()
      apply(machine.clear())
      return
    }
    currentRequest = MediaRequest.make(uri: source.uri, userAgent: source.userAgent, referrer: source.referrer, vlcOptions: source.vlcOptions)
    guard let request = currentRequest else {
      retireEngine()
      failInvalidSource()
      return
    }
    loadOnFreshEngine(request, slaves: [])
    let autoPlay = store.settings.autoPlay
    let effects = machine.load(uri: source.uri, autoPlay: autoPlay)
    if autoPlay {
      play(entering: effects)
    } else {
      apply(effects)
    }
  }

  func play() {
    guard isActive else { return }
    session.userDidCommand()
    startOrResume()
  }

  func pause() {
    guard isActive else { return }
    session.userDidCommand()
    reopen.pausesOnceStarted = false
    pauseEngine()
  }

  func stop() {
    guard isActive, machine.canStop else { return }
    session.userDidCommand()
    reopen.cancelPendingStart()
    resumeGuard.reset()
    retireEngine()
    store.updateMedia { $0.currentTime = 0 }
    throttle.reset()
    apply(machine.requestStop())
    store.emit(\.timeUpdate, PlayerPayloads.timeUpdate(store.media))
  }

  func seek(toMs target: Double) {
    guard isActive else { return }
    let media = store.media
    switch machine.seekAction(targetMs: target, duration: media.duration, isSeekable: media.isSeekable) {
    case .none:
      return
    case .seek(let milliseconds):
      reopen.keepSeek(milliseconds)
      performSeek(milliseconds)
    case .restartThenSeek(let milliseconds):
      reopen.pendingSeekMs = milliseconds
      restartPlayback()
    }
  }

  func seek(byMs delta: Double) {
    seek(toMs: Double(store.media.currentTime) + delta)
  }

  func selectAudioTrack(_ id: Int) {
    guard isActive, TrackList.canSelect(id, in: store.media.tracks.audioTracks) else { return }
    reopen.forgetSelection(.audio)
    engine.selectAudioTrack(id)
    refreshReadout()
  }

  func selectSubtitleTrack(_ id: Int) {
    guard isActive, TrackList.canSelect(id, in: store.media.tracks.subtitleTracks) else { return }
    reopen.forgetSelection(.subtitle)
    engine.selectSubtitleTrack(id)
    refreshReadout()
  }

  func addSlave(_ kind: PlaybackSlave.Kind, uri: String, select: Bool) {
    guard isActive, let url = MediaUri.resolve(uri) else { return }
    let slave = PlaybackSlave(kind: kind, url: url, select: select)
    guard kind == .audio, !hasAddedAudio else {
      keepAddedSlave(slave)
      return
    }
    reload(adding: slave)
  }

  func instanceOptionsDidChange() {
    reload(adding: nil)
  }

  private var hasAddedAudio: Bool {
    addedSlaves.contains { $0.kind == .audio }
  }

  private var mediaRequest: MediaRequest? {
    currentRequest?.readingAddedAudio(hasAddedAudio)
  }

  private func keepAddedSlave(_ slave: PlaybackSlave) {
    addedSlaves.append(slave)
    attachSlave(slave)
  }

  private func reload(adding slave: PlaybackSlave?) {
    guard isActive else { return }
    switch machine.reloadMode {
    case .none:
      slave.map(keepAddedSlave)
    case .withoutPlaying:
      slave.map(keepAddedSlave)
      if engineInstance != nil, let request = mediaRequest {
        loadOnFreshEngine(request, slaves: [])
      }
    case .inPlace:
      reloadInPlace(adding: slave)
    }
  }

  private func reloadInPlace(adding slave: PlaybackSlave?) {
    reopen.beginReload(
      media: store.media,
      hasReachedPlaying: machine.hasReachedPlaying,
      isPaused: machine.status == .paused,
      addedSlaves: addedSlaves
    )
    if let slave {
      addedSlaves.append(slave)
      reopen.add(slave)
    }
    guard let request = mediaRequest else { return }
    bufferingGate.reset()
    loadOnFreshEngine(request, slaves: reopen.slaves)
    play(entering: machine.beginPlayback())
  }

  private func attachSlave(_ slave: PlaybackSlave) {
    if machine.isMediaActive && engine.addSlave(slave) {
      return
    }
    reopen.add(slave)
  }

  func snapshot(_ promise: Promise<String>) {
    guard isActive, let engineInstance, surface.hasHosts, Self.startedStatuses.contains(machine.status), store.media.videoSize.isRenderable else {
      SnapshotRequests.reject(promise)
      return
    }
    let path = SnapshotRequests.makeTemporaryPath()
    let snapshots = snapshots
    snapshots.track(path: path, promise: promise)
    Self.snapshotQueue.async { [engineInstance] in
      let requested = engineInstance.saveSnapshot(toPath: path)
      DispatchQueue.main.async {
        snapshots.engineDidFinishRequest(path: path, succeeded: requested)
      }
    }
  }

  func applySettings() {
    guard isActive else { return }
    if machine.isMediaActive {
      engineInstance?.apply(store.settings)
    }
    session.settingsDidChange()
  }

  func attach(_ host: VideoHost) {
    guard isActive else { return }
    surface.attach(host)
  }

  func detach(_ host: VideoHost) {
    surface.detach(host)
    guard !isShutDown else { return }
    surface.mount()
  }

  func hostDidLayout(_ host: VideoHost) {
    guard isActive else { return }
    surface.hostDidLayout(host)
  }

  func shutdown() {
    guard !isShutDown else { return }
    isShutDown = true
    resumeGuard.reset()
    snapshots.rejectAll()
    retireEngine()
    session.shutdown()
    audioSession.deactivate()
    surface.removeAll()
  }

  private func startOrResume() {
    reopen.pausesOnceStarted = false
    switch machine.playAction() {
    case .none:
      return
    case .start:
      play(entering: machine.beginPlayback())
    case .resume:
      resume()
    case .restart:
      restartPlayback()
    case .reload:
      reloadAndPlay()
    }
  }

  private func resume() {
    audioSession.activate()
    let media = store.media
    let target = ResumeTarget(
      isNetworkSource: currentRequest.map { MediaUri.isNetwork($0.url.absoluteString) } ?? false,
      isLive: media.isLive,
      isSeekable: media.isSeekable
    )
    let plan = resumeGuard.resume(atMs: Self.nowMs, positionMs: media.currentTime, settings: store.settings, target: target)
    if let reconnectAtMs = plan.reconnectAtMs {
      engine.seek(toMs: reconnectAtMs)
    }
    engine.play()
    plan.stallCheck.map(scheduleStallCheck)
  }

  private func scheduleStallCheck(_ check: StallCheck) {
    DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(check.delayMs)) { [weak self] in
      self?.reloadIfStalled(check.generation)
    }
  }

  private func reloadIfStalled(_ generation: Int) {
    guard isActive,
          resumeGuard.isStalled(generation: generation, status: machine.status, positionMs: store.media.currentTime) else { return }
    reload(adding: nil)
  }

  private func pauseEngine() {
    guard machine.canPause else { return }
    engine.pause()
  }

  private func reloadAndPlay() {
    resetMediaState()
    guard let request = mediaRequest else {
      failInvalidSource()
      return
    }
    loadOnFreshEngine(request, slaves: addedSlaves)
    play(entering: machine.reloadForRetry())
  }

  private func restartPlayback() {
    guard let request = mediaRequest else {
      failInvalidSource()
      return
    }
    restartOnFreshEngine(request)
    play(entering: machine.beginPlayback())
  }

  private func restartOnFreshEngine(_ request: MediaRequest) {
    reopen.beginRestart(tracks: store.media.tracks, addedSlaves: addedSlaves)
    loadOnFreshEngine(request, slaves: reopen.slaves)
  }

  private func play(entering transition: [PlaybackEffect]) {
    audioSession.activate()
    apply(transition)
    engine.play()
  }

  private func loadOnFreshEngine(_ request: MediaRequest, slaves: [PlaybackSlave]) {
    let fresh = replaceEngine()
    fresh.apply(store.settings)
    fresh.load(request)
    slaves.forEach { _ = fresh.addSlave($0) }
  }

  private func replaceEngine() -> VlcEngine {
    retireEngine()
    let fresh = engine
    surface.applyResize(force: true)
    return fresh
  }

  private func retireEngine() {
    engineInstance?.retire()
    engineInstance = nil
    surface.engine = nil
  }

  private func failInvalidSource() {
    store.updateMedia { $0.error = PlayerErrors.invalidSource }
    apply(machine.failInvalidSource())
  }

  private func performSeek(_ milliseconds: Int) {
    engine.seek(toMs: milliseconds)
    store.updateMedia { $0.currentTime = milliseconds }
    resumeGuard.seek(toMs: milliseconds, stallAfterMs: store.settings.reloadOnStallAfterMs).map(scheduleStallCheck)
    throttle.markEmitted(atMs: Self.nowMs)
    store.emit(\.timeUpdate, PlayerPayloads.timeUpdate(store.media))
    session.refresh()
  }

  private func resetMediaState() {
    reopen = ReopenState()
    resumeGuard.reset()
    bufferingGate.reset()
    throttle.reset()
    videoSizeGate.reset()
    surface.container.isHidden = true
    store.updateMedia { media in
      let status = media.status
      media = MediaState()
      media.status = status
    }
    emitTracksIfChanged(TracksSnapshot())
  }

  private func apply(_ effects: [PlaybackEffect]) {
    for effect in effects {
      switch effect {
      case .status(let status): store.updateMedia { $0.status = status }
      case .error(let failure): store.updateMedia { $0.error = failure }
      case .startedPlaying, .loaded, .ended, .restartForLoop: break
      }
    }
    for effect in effects {
      switch effect {
      case .startedPlaying:
        didStartPlaying()
      case .status(let status):
        resumeGuard.statusChanged(status, atMs: Self.nowMs)
        if status == .playing {
          session.didReachPlaying()
        }
        store.emit(\.statusChange, PlayerPayloads.status(status))
      case .loaded:
        store.emit(\.loaded, PlayerPayloads.loaded(store.media))
      case .ended:
        store.emit(\.ended, ())
      case .error(let failure):
        store.emit(\.error, PlayerPayloads.error(failure))
      case .restartForLoop:
        DispatchQueue.main.async { [weak self, weak endedEngine = engineInstance] in
          self?.restartForLoop(after: endedEngine)
        }
      }
    }
    guard !effects.isEmpty else { return }
    session.refresh()
  }

  private func didStartPlaying() {
    guard let engineInstance else { return }
    reopen.playbackStarted()
    engineInstance.apply(store.settings)
    surface.applyResize(force: true)
    refreshReadout()
    reopen.takeSlavesToAttach().forEach(attachSlave)
    pauseIfRequestedWhileStarting()
  }

  private func applyPendingSeekOnceStarted() {
    guard let target = reopen.pendingSeekMs, machine.status == .playing, store.media.isSeekable else { return }
    reopen.pendingSeekMs = nil
    performSeek(target)
    pauseIfRequestedWhileStarting()
  }

  private func pauseIfRequestedWhileStarting() {
    guard reopen.pausesOnceStarted, reopen.pendingSeekMs == nil else { return }
    reopen.pausesOnceStarted = false
    engineInstance?.pause()
  }

  private func restartForLoop(after endedEngine: VlcEngine?) {
    guard isActive, machine.isRestartingForLoop, let endedEngine, endedEngine === engineInstance,
          let request = mediaRequest else { return }
    restartOnFreshEngine(request)
    store.updateMedia { $0.currentTime = 0 }
    engine.play()
    session.refresh()
  }

  private func refreshReadout() {
    guard let engineInstance, machine.isMediaActive, !machine.isRestartingForLoop else { return }
    let readout = engineInstance.readout
    let hasReachedPlaying = machine.hasReachedPlaying
    let acceptsLengthAndSeekable = hasReachedPlaying && engineInstance.hasPlayed && Self.startedStatuses.contains(machine.status)
    store.updateMedia { media in
      media.currentTime = max(readout.timeMs, 0)
      if acceptsLengthAndSeekable {
        media.duration = VlcContract.duration(fromEngineLength: readout.lengthMs)
        media.isSeekable = readout.isSeekable
      }
      media.isLive = VlcContract.isLive(duration: media.duration, isSeekable: media.isSeekable, hasReachedPlaying: hasReachedPlaying)
      media.videoSize = readout.videoSize
      media.tracks = readout.tracks
    }
    emitTracksIfChanged(readout.tracks)
    restoreTrackSelection(readout.tracks)
    revealVideoOnceRendered(readout.videoSize)
    if videoSizeGate.shouldEmit(readout.videoSize) {
      store.emit(\.videoSizeChange, PlayerPayloads.videoSize(readout.videoSize))
      surface.applyResize(force: false)
    }
  }

  private func revealVideoOnceRendered(_ size: VideoDimensions) {
    guard surface.container.isHidden, size.isRenderable, machine.hasReachedPlaying else { return }
    surface.container.isHidden = false
  }

  private func restoreTrackSelection(_ tracks: TracksSnapshot) {
    let restored = reopen.tracksArrived(tracks, isPlaying: machine.status == .playing)
    if let audio = restored.audio {
      engineInstance?.selectAudioTrack(audio)
    }
    if let subtitle = restored.subtitle {
      engineInstance?.selectSubtitleTrack(subtitle)
    }
  }

  private func emitTracksIfChanged(_ tracks: TracksSnapshot) {
    guard tracksGate.shouldEmit(tracks) else { return }
    store.emit(\.tracksChange, PlayerPayloads.tracksInfo(tracks))
  }

  private static var nowMs: Double {
    ProcessInfo.processInfo.systemUptime * 1000
  }
}

extension PlaybackController: VlcEngineDelegate {
  func engineDidChangeState(_ state: EngineState, isPlaying: Bool) {
    guard isActive else { return }
    if session.isVideoDisabled {
      engineInstance?.disableVideoTrack()
    }
    apply(machine.handle(engine: state, engineIsPlaying: isPlaying, loop: store.settings.loop))
    refreshReadout()
  }

  func engineDidChangeTime() {
    guard isActive else { return }
    refreshReadout()
    applyPendingSeekOnceStarted()
    let settings = store.settings
    guard machine.status == .playing, throttle.shouldEmit(atMs: Self.nowMs, intervalMs: settings.timeUpdateInterval) else { return }
    store.emit(\.timeUpdate, PlayerPayloads.timeUpdate(store.media))
  }

  func engineDidTakeSnapshot() {
    snapshots.engineDidTakeSnapshot()
  }

  func engineDidReportBuffering(percent: Float) {
    guard isActive, machine.isMediaActive,
          let rounded = VlcContract.bufferingPercent(fromEngineCache: percent),
          bufferingGate.shouldEmit(rounded) else { return }
    store.emit(\.buffering, PlayerPayloads.buffering(rounded))
  }
}

extension PlaybackController: MediaSessionControl {
  func pauseForSession() {
    guard isActive else { return }
    if machine.canPause {
      pauseEngine()
    } else if machine.status == .opening {
      reopen.pausesOnceStarted = true
    }
  }

  func resumeForSession() {
    guard isActive else { return }
    startOrResume()
  }

  func setVideoTrackEnabled(_ enabled: Bool) {
    guard isActive else { return }
    if enabled {
      engineInstance?.restoreVideoTrack()
    } else {
      engineInstance?.disableVideoTrack()
    }
  }
}
