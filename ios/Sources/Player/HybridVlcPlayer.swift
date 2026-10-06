import NitroModules

final class HybridVlcPlayer: HybridVlcPlayerSpec {
  private static let engineMemoryFootprint = 8 * 1024 * 1024

  private let store = PlayerStore()
  private let controller: PlaybackController

  override init() {
    controller = PlaybackController(store: store)
    super.init()
  }

  deinit {
    shutDownController()
  }

  var memorySize: Int {
    store.isReleased ? 0 : Self.engineMemoryFootprint
  }

  var source: VlcSource? {
    get { store.source }
    set {
      guard store.setSource(newValue) else { return }
      onMain { $0.load(newValue) }
    }
  }

  var volume: Double {
    get { Double(store.settings.volume) }
    set { updateSetting(VlcContract.clampVolume(newValue)) { $0.volume = $1 } }
  }

  var muted: Bool {
    get { store.settings.muted }
    set { updateSetting(newValue) { $0.muted = $1 } }
  }

  var rate: Double {
    get { store.settings.rate }
    set { updateSetting(VlcContract.clampRate(newValue)) { $0.rate = $1 } }
  }

  var loop: Bool {
    get { store.settings.loop }
    set { updateSetting(newValue) { $0.loop = $1 } }
  }

  var autoPlay: Bool {
    get { store.settings.autoPlay }
    set { updateSetting(newValue) { $0.autoPlay = $1 } }
  }

  var timeUpdateInterval: Double {
    get { Double(store.settings.timeUpdateInterval) }
    set { updateSetting(VlcContract.clampTimeUpdateInterval(newValue)) { $0.timeUpdateInterval = $1 } }
  }

  var subtitleDelay: Double {
    get { Double(store.settings.subtitleDelay) }
    set { updateSetting(VlcContract.clampDelay(newValue)) { $0.subtitleDelay = $1 } }
  }

  var audioDelay: Double {
    get { Double(store.settings.audioDelay) }
    set { updateSetting(VlcContract.clampDelay(newValue)) { $0.audioDelay = $1 } }
  }

  var status: VlcStatus { PlayerPayloads.status(store.media.status) }
  var error: VlcError? { store.media.error.map(PlayerPayloads.error) }
  var currentTime: Double { Double(store.media.currentTime) }
  var duration: Double { Double(store.media.duration) }
  var isLive: Bool { store.media.isLive }
  var isSeekable: Bool { store.media.isSeekable }
  var videoSize: VlcVideoSize { PlayerPayloads.videoSize(store.media.videoSize) }
  var audioTracks: [VlcTrack] { PlayerPayloads.tracks(store.media.tracks.audioTracks) }
  var subtitleTracks: [VlcTrack] { PlayerPayloads.tracks(store.media.tracks.subtitleTracks) }
  var selectedAudioTrack: Double { Double(store.media.tracks.selectedAudioTrack) }
  var selectedSubtitleTrack: Double { Double(store.media.tracks.selectedSubtitleTrack) }

  func play() throws {
    onMain { $0.play() }
  }

  func pause() throws {
    onMain { $0.pause() }
  }

  func stop() throws {
    onMain { $0.stop() }
  }

  func seek(timeMs: Double) throws {
    onMain { $0.seek(toMs: timeMs) }
  }

  func seekBy(deltaMs: Double) throws {
    onMain { $0.seek(byMs: deltaMs) }
  }

  func setAudioTrack(id: Double) throws {
    onMain { $0.selectAudioTrack(Int(id)) }
  }

  func setSubtitleTrack(id: Double) throws {
    onMain { $0.selectSubtitleTrack(Int(id)) }
  }

  func addSubtitle(uri: String, select: Bool) throws {
    guard !store.isReleased else { return }
    guard store.source != nil else {
      throw RuntimeError.error(withMessage: PlayerErrors.addSubtitleWithoutSource)
    }
    onMain { $0.addSubtitle(uri: uri, select: select) }
  }

  func snapshot() throws -> Promise<String> {
    let promise = Promise<String>()
    guard !store.isReleased else {
      SnapshotRequests.reject(promise)
      return promise
    }
    let controller = controller
    DispatchQueue.main.async {
      controller.snapshot(promise)
    }
    return promise
  }

  func release() throws {
    guard store.markReleased() else { return }
    shutDownController()
  }

  func dispose() {
    try? release()
  }

  func addOnStatusChangeListener(listener: @escaping (VlcStatus) -> Void) throws -> VlcListenerSubscription {
    store.events.statusChange.add(listener)
  }

  func addOnTimeUpdateListener(listener: @escaping (VlcTimeUpdate) -> Void) throws -> VlcListenerSubscription {
    store.events.timeUpdate.add(listener)
  }

  func addOnLoadedListener(listener: @escaping (VlcLoadedInfo) -> Void) throws -> VlcListenerSubscription {
    store.events.loaded.add(listener)
  }

  func addOnEndedListener(listener: @escaping () -> Void) throws -> VlcListenerSubscription {
    store.events.ended.add { listener() }
  }

  func addOnErrorListener(listener: @escaping (VlcError) -> Void) throws -> VlcListenerSubscription {
    store.events.error.add(listener)
  }

  func addOnVolumeChangeListener(listener: @escaping (VlcVolumeInfo) -> Void) throws -> VlcListenerSubscription {
    store.events.volumeChange.add(listener)
  }

  func addOnTracksChangeListener(listener: @escaping (VlcTracksInfo) -> Void) throws -> VlcListenerSubscription {
    store.events.tracksChange.add(listener)
  }

  func addOnVideoSizeChangeListener(listener: @escaping (VlcVideoSize) -> Void) throws -> VlcListenerSubscription {
    store.events.videoSizeChange.add(listener)
  }

  func attach(_ host: VideoHost) {
    controller.attach(host)
  }

  func detach(_ host: VideoHost) {
    controller.detach(host)
  }

  func hostDidLayout(_ host: VideoHost) {
    controller.hostDidLayout(host)
  }

  private func onMain(_ work: @escaping (PlaybackController) -> Void) {
    guard !store.isReleased else { return }
    let controller = controller
    DispatchQueue.main.async {
      work(controller)
    }
  }

  private func shutDownController() {
    let controller = controller
    MainThread.runNowOrAsync {
      controller.shutdown()
    }
  }

  private func updateSetting<Value>(_ value: Value?, _ assign: (inout PlayerSettings, Value) -> Void) {
    guard let value, let change = store.updateSettings({ assign(&$0, value) }) else { return }
    guard change.before != change.after else { return }
    if change.before.volumeState != change.after.volumeState {
      store.emit(\.volumeChange, PlayerPayloads.volume(change.after.volumeState))
    }
    onMain { $0.applySettings() }
  }
}
