import MobileVLCKit
import UIKit

protocol VlcEngineDelegate: AnyObject {
  func engineDidChangeState(_ state: EngineState, isPlaying: Bool)
  func engineDidChangeTime()
  func engineDidTakeSnapshot()
}

struct EngineReadout {
  let timeMs: Int
  let lengthMs: Int
  let isSeekable: Bool
  let videoSize: VideoDimensions
  let tracks: TracksSnapshot
}

final class VlcEngine: NSObject, VLCMediaPlayerDelegate {
  private static let retirementQueue = DispatchQueue(label: "rn-vlc-plyr.engine-retirement")
  private static let retirementGracePeriod: TimeInterval = 1

  weak var delegate: VlcEngineDelegate?
  private let player = VLCMediaPlayer()

  init(drawable: UIView) {
    super.init()
    player.delegate = self
    player.drawable = drawable
  }

  func load(_ request: MediaRequest) {
    let media = VLCMedia(url: request.url)
    request.options.forEach { media.addOption($0) }
    player.media = media
  }

  func play() {
    player.play()
  }

  func pause() {
    player.pause()
  }

  func stop() {
    player.stop()
  }

  func restartFromBeginning() {
    player.stop()
    player.play()
  }

  func seek(toMs milliseconds: Int) {
    player.time = VLCTime(number: NSNumber(value: milliseconds))
  }

  func apply(_ settings: PlayerSettings) {
    if let audio = player.audio as VLCAudio? {
      audio.volume = Int32(settings.volume)
      audio.isMuted = settings.muted
    }
    player.rate = Float(settings.rate)
    player.currentVideoSubTitleDelay = VlcContract.engineDelay(fromMilliseconds: settings.subtitleDelay)
    player.currentAudioPlaybackDelay = VlcContract.engineDelay(fromMilliseconds: settings.audioDelay)
  }

  func apply(_ resize: ResizeSettings) {
    player.scaleFactor = resize.scaleFactor
    withOptionalCString(resize.aspectRatio) { player.videoAspectRatio = $0 }
    withOptionalCString(resize.cropGeometry) { player.videoCropGeometry = $0 }
  }

  func selectAudioTrack(_ id: Int) {
    player.currentAudioTrackIndex = Int32(id)
  }

  func selectSubtitleTrack(_ id: Int) {
    player.currentVideoSubTitleIndex = Int32(id)
  }

  func addSubtitle(_ url: URL, select: Bool) -> Bool {
    player.addPlaybackSlave(url, type: .subtitle, enforce: select) == 0
  }

  func saveSnapshot(toPath path: String) -> Bool {
    VlcSnapshotRequest.saveSnapshot(of: player, toPath: path)
  }

  var readout: EngineReadout {
    EngineReadout(
      timeMs: player.time.value?.intValue ?? 0,
      lengthMs: player.media?.length.value?.intValue ?? 0,
      isSeekable: player.isSeekable,
      videoSize: VideoDimensions(width: Int(player.videoSize.width), height: Int(player.videoSize.height)),
      tracks: TracksSnapshot(
        audioTracks: TrackList.build(ids: Self.ids(player.audioTrackIndexes), names: Self.names(player.audioTrackNames)),
        subtitleTracks: TrackList.build(ids: Self.ids(player.videoSubTitlesIndexes), names: Self.names(player.videoSubTitlesNames)),
        selectedAudioTrack: Int(player.currentAudioTrackIndex),
        selectedSubtitleTrack: Int(player.currentVideoSubTitleIndex)
      )
    )
  }

  func retire() {
    delegate = nil
    player.delegate = nil
    Self.stopAndReleaseOffMainThread(player)
  }

  private static func stopAndReleaseOffMainThread(_ player: VLCMediaPlayer) {
    let retained = Unmanaged.passRetained(player)
    retirementQueue.async {
      retained.takeUnretainedValue().stop()
    }
    DispatchQueue.main.asyncAfter(deadline: .now() + retirementGracePeriod) {
      retirementQueue.async {
        retained.release()
      }
    }
  }

  func mediaPlayerStateChanged(_ aNotification: Notification) {
    delegate?.engineDidChangeState(Self.engineState(player.state), isPlaying: player.isPlaying)
  }

  func mediaPlayerTimeChanged(_ aNotification: Notification) {
    delegate?.engineDidChangeTime()
  }

  func mediaPlayerSnapshot(_ aNotification: Notification) {
    delegate?.engineDidTakeSnapshot()
  }

  private func withOptionalCString(_ value: String?, _ apply: (UnsafeMutablePointer<CChar>?) -> Void) {
    guard let value, let pointer = strdup(value) else {
      apply(nil)
      return
    }
    apply(pointer)
    free(pointer)
  }

  private static func ids(_ values: [Any]?) -> [Int] {
    (values ?? []).compactMap { ($0 as? NSNumber)?.intValue }
  }

  private static func names(_ values: [Any]?) -> [String] {
    (values ?? []).map { ($0 as? String) ?? "" }
  }

  private static func engineState(_ state: VLCMediaPlayerState) -> EngineState {
    switch state {
    case .stopped: return .stopped
    case .opening: return .opening
    case .buffering: return .buffering
    case .ended: return .ended
    case .error: return .error
    case .playing: return .playing
    case .paused: return .paused
    case .esAdded: return .esAdded
    @unknown default: return .esAdded
    }
  }
}
