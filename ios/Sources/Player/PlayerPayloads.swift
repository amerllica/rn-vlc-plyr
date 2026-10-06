import Foundation

enum PlayerPayloads {
  static func tracks(_ tracks: [TrackInfo]) -> [VlcTrack] {
    tracks.map { VlcTrack(id: Double($0.id), name: $0.name) }
  }

  static func error(_ failure: PlayerFailure) -> VlcError {
    VlcError(code: errorCode(failure.code), message: failure.message)
  }

  static func status(_ status: PlayerStatus) -> VlcStatus {
    switch status {
    case .idle: return .idle
    case .opening: return .opening
    case .buffering: return .buffering
    case .playing: return .playing
    case .paused: return .paused
    case .stopped: return .stopped
    case .ended: return .ended
    case .error: return .error
    }
  }

  private static func errorCode(_ code: PlayerErrorCode) -> VlcErrorCode {
    switch code {
    case .invalidSource: return .invalidsource
    case .network: return .network
    case .media: return .media
    case .unknown: return .unknown
    }
  }

  static func videoSize(_ size: VideoDimensions) -> VlcVideoSize {
    VlcVideoSize(width: Double(size.width), height: Double(size.height))
  }

  static func timeUpdate(_ media: MediaState) -> VlcTimeUpdate {
    VlcTimeUpdate(currentTime: Double(media.currentTime), duration: Double(media.duration))
  }

  static func volume(_ state: VolumeState) -> VlcVolumeInfo {
    VlcVolumeInfo(volume: Double(state.volume), muted: state.muted)
  }

  static func tracksInfo(_ snapshot: TracksSnapshot) -> VlcTracksInfo {
    VlcTracksInfo(
      audioTracks: tracks(snapshot.audioTracks),
      subtitleTracks: tracks(snapshot.subtitleTracks),
      selectedAudioTrack: Double(snapshot.selectedAudioTrack),
      selectedSubtitleTrack: Double(snapshot.selectedSubtitleTrack)
    )
  }

  static func loaded(_ media: MediaState) -> VlcLoadedInfo {
    VlcLoadedInfo(
      duration: Double(media.duration),
      isSeekable: media.isSeekable,
      isLive: media.isLive,
      videoSize: videoSize(media.videoSize),
      audioTracks: tracks(media.tracks.audioTracks),
      subtitleTracks: tracks(media.tracks.subtitleTracks)
    )
  }
}
