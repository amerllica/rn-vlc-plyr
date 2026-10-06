import NitroModules
import UIKit

protocol VideoHost: AnyObject {
  var hostView: UIView { get }
  var hostResizeMode: ResizeMode { get }
}

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
  private var hosts = SurfaceStack<AnyObject>()
  private let snapshots = SnapshotRequests()
  private var engineInstance: VlcEngine?
  private var currentRequest: MediaRequest?
  private var pendingSeekMs: Int?
  private var pendingSubtitles: [(url: URL, select: Bool)] = []
  private var isShutDown = false
  private var lastResize: ResizeSettings?
  private lazy var videoContainer = Self.makeVideoContainer()

  init(store: PlayerStore) {
    self.store = store
  }

  private var topHost: VideoHost? {
    hosts.top as? VideoHost
  }

  private var isActive: Bool {
    !isShutDown && !store.isReleased
  }

  private var engine: VlcEngine {
    if let engineInstance { return engineInstance }
    let created = VlcEngine(drawable: videoContainer)
    created.delegate = self
    engineInstance = created
    return created
  }

  func load(_ source: VlcSource?) {
    guard isActive else { return }
    resetMediaState()
    guard let source else {
      currentRequest = nil
      engineInstance?.unload()
      apply(machine.clear())
      return
    }
    currentRequest = MediaRequest.make(uri: source.uri, userAgent: source.userAgent, referrer: source.referrer, vlcOptions: source.vlcOptions)
    guard let request = currentRequest else {
      engineInstance?.unload()
      failInvalidSource()
      return
    }
    engine.load(request)
    let autoPlay = store.settings.autoPlay
    if autoPlay {
      audioSession.activateOnce()
    }
    apply(machine.load(uri: source.uri, autoPlay: autoPlay))
    if autoPlay {
      engine.play()
    }
  }

  func play() {
    guard isActive else { return }
    switch machine.playAction() {
    case .none:
      return
    case .start:
      startPlayback()
    case .resume:
      audioSession.activateOnce()
      engine.play()
    case .restart:
      engine.stop()
      startPlayback()
    case .reload:
      reloadAndPlay()
    }
  }

  func pause() {
    guard isActive, machine.canPause else { return }
    engine.pause()
  }

  func stop() {
    guard isActive, machine.canStop else { return }
    pendingSeekMs = nil
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
    engine.selectAudioTrack(id)
    refreshReadout()
  }

  func selectSubtitleTrack(_ id: Int) {
    guard isActive, TrackList.canSelect(id, in: store.media.tracks.subtitleTracks) else { return }
    engine.selectSubtitleTrack(id)
    refreshReadout()
  }

  func addSubtitle(uri: String, select: Bool) {
    guard isActive, let url = MediaUri.resolve(uri) else { return }
    let isMediaOpen = Self.mediaActiveStatuses.contains(machine.status)
    if isMediaOpen && engine.addSubtitle(url, select: select) {
      return
    }
    pendingSubtitles.append((url, select))
  }

  func snapshot(_ promise: Promise<String>) {
    guard isActive, let engineInstance, !hosts.isEmpty, Self.snapshotStatuses.contains(machine.status), store.media.videoSize.isRenderable else {
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
    guard isActive, Self.mediaActiveStatuses.contains(machine.status) else { return }
    engineInstance?.apply(store.settings)
  }

  func attach(_ host: VideoHost) {
    guard isActive else { return }
    hosts.attach(host)
    mountVideoContainer()
  }

  func detach(_ host: VideoHost) {
    hosts.detach(host)
    guard !isShutDown else { return }
    mountVideoContainer()
  }

  func hostDidLayout(_ host: VideoHost) {
    guard isActive, topHost === host else { return }
    mountVideoContainer()
  }

  func shutdown() {
    guard !isShutDown else { return }
    isShutDown = true
    snapshots.rejectAll()
    engineInstance?.retire()
    engineInstance = nil
    hosts.removeAll()
    videoContainer.removeFromSuperview()
  }

  private func reloadAndPlay() {
    resetMediaState()
    guard let request = currentRequest else {
      failInvalidSource()
      return
    }
    engine.load(request)
    audioSession.activateOnce()
    apply(machine.reloadForRetry())
    engine.play()
  }

  private func failInvalidSource() {
    store.updateMedia { $0.error = PlayerErrors.invalidSource }
    apply(machine.failInvalidSource())
  }

  private func startPlayback() {
    audioSession.activateOnce()
    apply(machine.beginPlayback())
    engine.play()
  }

  private func performSeek(_ milliseconds: Int) {
    engine.seek(toMs: milliseconds)
    store.updateMedia { $0.currentTime = milliseconds }
    throttle.markEmitted(atMs: Self.nowMs)
    store.emit(\.timeUpdate, PlayerPayloads.timeUpdate(store.media))
  }

  private func resetMediaState() {
    pendingSeekMs = nil
    pendingSubtitles.removeAll()
    throttle.reset()
    videoSizeGate.reset()
    videoContainer.isHidden = true
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
  }

  private func didEnterPlaying() {
    guard let engineInstance else { return }
    engineInstance.apply(store.settings)
    applyResize(force: true)
    refreshReadout()
    let subtitles = pendingSubtitles
    pendingSubtitles.removeAll()
    subtitles.forEach { addSubtitle(uri: $0.url.absoluteString, select: $0.select) }
  }

  private func applyPendingSeekOnceStarted() {
    guard let pendingSeekMs, machine.status == .playing else { return }
    self.pendingSeekMs = nil
    performSeek(pendingSeekMs)
  }

  private func restartForLoop() {
    guard isActive else { return }
    engineInstance?.restartFromBeginning()
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
    revealVideoOnceRendered(readout.videoSize)
    if videoSizeGate.shouldEmit(readout.videoSize) {
      store.emit(\.videoSizeChange, PlayerPayloads.videoSize(readout.videoSize))
      applyResize(force: false)
    }
  }

  private func revealVideoOnceRendered(_ size: VideoDimensions) {
    guard videoContainer.isHidden, size.isRenderable, machine.hasReachedPlaying else { return }
    videoContainer.isHidden = false
  }

  private func emitTracksIfChanged(_ tracks: TracksSnapshot) {
    guard tracksGate.shouldEmit(tracks) else { return }
    store.emit(\.tracksChange, PlayerPayloads.tracksInfo(tracks))
  }

  private func mountVideoContainer() {
    guard let host = topHost else {
      videoContainer.removeFromSuperview()
      return
    }
    let isNewHost = videoContainer.superview !== host.hostView
    if isNewHost {
      host.hostView.insertSubview(videoContainer, at: 0)
    }
    let sizeChanged = videoContainer.frame != host.hostView.bounds
    videoContainer.frame = host.hostView.bounds
    if isNewHost || sizeChanged {
      relayoutRenderedVideo()
    }
    applyResize(force: isNewHost || sizeChanged)
  }

  private func relayoutRenderedVideo() {
    videoContainer.subviews.forEach { renderView in
      renderView.frame = videoContainer.bounds
      renderView.setNeedsLayout()
    }
    videoContainer.layoutIfNeeded()
  }

  private func applyResize(force: Bool) {
    guard let engineInstance, let host = topHost else { return }
    let settings = ResizeGeometry.settings(mode: host.hostResizeMode, viewSize: host.hostView.bounds.size)
    guard force || settings != lastResize else { return }
    lastResize = settings
    engineInstance.apply(settings)
  }

  private static var nowMs: Double {
    ProcessInfo.processInfo.systemUptime * 1000
  }

  private static func makeVideoContainer() -> UIView {
    let container = UIView()
    container.backgroundColor = .black
    container.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    container.isUserInteractionEnabled = false
    return container
  }
}

extension PlaybackController: VlcEngineDelegate {
  func engineDidChangeState(_ state: EngineState, isPlaying: Bool) {
    guard isActive else { return }
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
}
