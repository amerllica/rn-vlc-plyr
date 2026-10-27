import NitroModules
import UIKit

final class PlaybackController {
  private static let snapshotQueue = DispatchQueue(label: "rn-vlc-plyr.snapshot")
  private static let snapshotStatuses: Set<PlayerStatus> = [.buffering, .playing, .paused]
  private static let mediaActiveStatuses: Set<PlayerStatus> = [.opening, .buffering, .playing, .paused]

  private let store: PlayerStore
  private var machine = PlaybackStateMachine()
  private var tracksGate = ChangeGate(initial: TracksSnapshot())
  private var videoSizeGate = VideoSizeGate()
  private var throttle = TimeUpdateThrottle()
  private var audioSession = PlaybackAudioSession()
  private let snapshots = SnapshotRequests()
  private var engineInstance: VlcEngine?
  private var currentRequest: MediaRequest?
  private var pendingSeekMs: Int?
  private var pendingSlaves: [PlaybackSlave] = []
  private var addedSlaves: [PlaybackSlave] = []
  private var reopenSlaves: [PlaybackSlave] = []
  private var trackRestore = TrackSelectionRestore()
  private var isReloadingInPlace = false
  private var tracksBeforeReopen: TracksSnapshot?
  private var pausesOnceStarted = false
  private var resumeReconnect = ResumeReconnect()
  private var stallWatchdog = StallWatchdog()
  private var stallCheck: DispatchWorkItem?
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
    let settings = store.settings
    let created = VlcEngine(
      drawable: surface.container,
      libraryOptions: VlcContract.instanceOptions(libvlcOptions: settings.libvlcOptions, subtitleStyle: settings.subtitleStyle)
    )
    created.delegate = self
    engineInstance = created
    surface.engine = created
    return created
  }

  func load(_ source: VlcSource?) {
    guard isActive else { return }
    defer { session.sourceDidChange() }
    resetMediaState()
    resumeReconnect.clear()
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
    replaceEngine().load(request)
    let autoPlay = store.settings.autoPlay
    if autoPlay {
      audioSession.activate()
    }
    apply(machine.load(uri: source.uri, autoPlay: autoPlay))
    if autoPlay {
      engine.play()
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
    pausesOnceStarted = false
    pauseEngine()
  }

  func stop() {
    guard isActive, machine.canStop else { return }
    session.userDidCommand()
    pausesOnceStarted = false
    isReloadingInPlace = false
    pendingSeekMs = nil
    resumeReconnect.clear()
    cancelStallCheck()
    engineInstance?.stop()
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
      performSeek(milliseconds)
    case .restartThenSeek(let milliseconds):
      pendingSeekMs = milliseconds
      engine.stop()
      startPlayback()
    }
  }

  func seek(byMs delta: Double) {
    seek(toMs: Double(store.media.currentTime) + delta)
  }

  func selectAudioTrack(_ id: Int) {
    guard isActive, TrackList.canSelect(id, in: store.media.tracks.audioTracks) else { return }
    tracksBeforeReopen = nil
    engine.selectAudioTrack(id)
    refreshReadout()
  }

  func selectSubtitleTrack(_ id: Int) {
    guard isActive, TrackList.canSelect(id, in: store.media.tracks.subtitleTracks) else { return }
    tracksBeforeReopen = nil
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

  func subtitleStyleDidChange() {
    reload(adding: nil)
  }

  func libvlcOptionsDidChange() {
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
    guard isActive, engineInstance != nil, currentRequest != nil, machine.status != .error else {
      slave.map(keepAddedSlave)
      return
    }
    guard Self.mediaActiveStatuses.contains(machine.status) else {
      slave.map(keepAddedSlave)
      mediaRequest.map { replaceEngine().load($0) }
      return
    }
    if !isReloadingInPlace {
      captureStateForReopen()
      isReloadingInPlace = true
    }
    if let slave {
      addedSlaves.append(slave)
      pendingSlaves.append(slave)
      if slave.select { trackRestore.forgetAudioSelection() }
    }
    guard let request = mediaRequest else { return }
    let fresh = replaceEngine()
    bufferingGate.reset()
    fresh.load(request)
    reopenSlaves.forEach { _ = fresh.addSlave($0) }
    audioSession.activate()
    apply(machine.beginPlayback())
    engine.play()
  }

  private func captureStateForReopen() {
    let media = store.media
    if pendingSeekMs == nil && machine.hasReachedPlaying && media.isSeekable && media.currentTime > 0 {
      pendingSeekMs = media.currentTime
    }
    pausesOnceStarted = pausesOnceStarted || machine.status == .paused
    let tracks = tracksBeforeReopen.flatMap { TrackList.isStillLoading(media.tracks, comparedTo: $0) ? $0 : nil } ?? media.tracks
    tracksBeforeReopen = tracks
    trackRestore = TrackSelectionRestore(capturing: tracks, slaves: addedSlaves)
    reopenSlaves = trackRestore.reopenSlaves
  }

  private func attachSlave(_ slave: PlaybackSlave) {
    let isMediaOpen = Self.mediaActiveStatuses.contains(machine.status)
    if isMediaOpen && engine.addSlave(slave) {
      return
    }
    pendingSlaves.append(slave)
  }

  func snapshot(_ promise: Promise<String>) {
    guard isActive, let engineInstance, surface.hasHosts, Self.snapshotStatuses.contains(machine.status), store.media.videoSize.isRenderable else {
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
    if Self.mediaActiveStatuses.contains(machine.status) {
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
    cancelStallCheck()
    snapshots.rejectAll()
    retireEngine()
    session.shutdown()
    audioSession.deactivate()
    surface.removeAll()
  }

  private func startOrResume() {
    pausesOnceStarted = false
    switch machine.playAction() {
    case .none:
      return
    case .start:
      startPlayback()
    case .resume:
      audioSession.activate()
      reconnectIfIdleTooLong()
      engine.play()
      scheduleStallCheck()
    case .restart:
      engine.stop()
      startPlayback()
    case .reload:
      reloadAndPlay()
    }
  }

  private func reconnectIfIdleTooLong() {
    let media = store.media
    let target = ResumeTarget(
      isNetworkSource: currentRequest.map { MediaUri.isNetwork($0.url.absoluteString) } ?? false,
      isLive: media.isLive,
      isSeekable: media.isSeekable
    )
    guard resumeReconnect.takeReconnect(atMs: Self.nowMs, afterMs: store.settings.reconnectOnResumeAfterMs, target: target) else { return }
    engine.seek(toMs: media.currentTime)
  }

  private func scheduleStallCheck() {
    cancelStallCheck()
    let delayMs = store.settings.reloadOnStallAfterMs
    guard delayMs > 0 else { return }
    stallWatchdog.arm(positionMs: store.media.currentTime)
    let check = DispatchWorkItem { [weak self] in
      self?.reloadIfStalled()
    }
    stallCheck = check
    DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(delayMs), execute: check)
  }

  private func reloadIfStalled() {
    stallCheck = nil
    guard isActive, stallWatchdog.isDue(status: machine.status, positionMs: store.media.currentTime) else { return }
    stallWatchdog.markFired()
    reload(adding: nil)
  }

  private func cancelStallCheck() {
    stallCheck?.cancel()
    stallCheck = nil
    stallWatchdog.disarm()
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
    replaceEngine().load(request)
    audioSession.activate()
    apply(machine.reloadForRetry())
    engine.play()
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

  private func startPlayback() {
    audioSession.activate()
    apply(machine.beginPlayback())
    engine.play()
  }

  private func performSeek(_ milliseconds: Int) {
    engine.seek(toMs: milliseconds)
    store.updateMedia { $0.currentTime = milliseconds }
    throttle.markEmitted(atMs: Self.nowMs)
    store.emit(\.timeUpdate, PlayerPayloads.timeUpdate(store.media))
    session.refresh()
  }

  private func resetMediaState() {
    pendingSeekMs = nil
    cancelStallCheck()
    bufferingGate.reset()
    pausesOnceStarted = false
    pendingSlaves.removeAll()
    reopenSlaves.removeAll()
    trackRestore = TrackSelectionRestore()
    isReloadingInPlace = false
    tracksBeforeReopen = nil
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
      if case .error(let failure) = effect {
        store.updateMedia { $0.error = failure }
      }
    }
    for effect in effects {
      switch effect {
      case .status(let status):
        store.updateMedia { $0.status = status }
        trackRecovery(for: status)
        store.emit(\.statusChange, PlayerPayloads.status(status))
        if status == .playing {
          didEnterPlaying()
        }
      case .loaded:
        store.emit(\.loaded, PlayerPayloads.loaded(store.media))
      case .ended:
        store.emit(\.ended, ())
      case .error(let failure):
        store.emit(\.error, PlayerPayloads.error(failure))
      case .restartForLoop:
        DispatchQueue.main.async { [weak self] in
          self?.restartForLoop()
        }
      }
    }
    guard !effects.isEmpty else { return }
    session.refresh()
  }

  private func trackRecovery(for status: PlayerStatus) {
    if status == .paused {
      resumeReconnect.didPause(atMs: Self.nowMs)
    }
    if StallWatchdog.ends(at: status) {
      cancelStallCheck()
    }
  }

  private func didEnterPlaying() {
    guard let engineInstance else { return }
    isReloadingInPlace = false
    engineInstance.apply(store.settings)
    surface.applyResize(force: true)
    refreshReadout()
    let slaves = pendingSlaves
    pendingSlaves.removeAll()
    slaves.forEach(attachSlave)
    if pausesOnceStarted && pendingSeekMs == nil {
      pausesOnceStarted = false
      engineInstance.pause()
    }
  }

  private func applyPendingSeekOnceStarted() {
    guard let pendingSeekMs, machine.status == .playing, store.media.isSeekable else { return }
    self.pendingSeekMs = nil
    performSeek(pendingSeekMs)
    if pausesOnceStarted {
      pausesOnceStarted = false
      engineInstance?.pause()
    }
  }

  private func restartForLoop() {
    guard isActive else { return }
    engineInstance?.restartFromBeginning()
    store.updateMedia { $0.currentTime = 0 }
    session.refresh()
  }

  private func refreshReadout() {
    guard let engineInstance, Self.mediaActiveStatuses.contains(machine.status), !machine.isRestartingForLoop else { return }
    let readout = engineInstance.readout
    let hasReachedPlaying = machine.hasReachedPlaying
    store.updateMedia { media in
      media.currentTime = max(readout.timeMs, 0)
      if hasReachedPlaying {
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
    if let before = tracksBeforeReopen, !TrackList.isStillLoading(tracks, comparedTo: before) {
      tracksBeforeReopen = nil
    }
    guard let engineInstance, machine.status == .playing else { return }
    if let id = trackRestore.takeAudioTrack(from: tracks.audioTracks), id != tracks.selectedAudioTrack {
      engineInstance.selectAudioTrack(id)
    }
    if let id = trackRestore.takeSubtitleTrack(from: tracks.subtitleTracks), id != tracks.selectedSubtitleTrack {
      engineInstance.selectSubtitleTrack(id)
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
    guard isActive, Self.mediaActiveStatuses.contains(machine.status),
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
      pausesOnceStarted = true
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
