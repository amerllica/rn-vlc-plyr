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

  static func nowPlaying(_ info: NowPlayingInfo) -> VlcNowPlaying {
    VlcNowPlaying(title: info.title, artist: info.artist, album: info.album, artworkUri: info.artworkUri)
  }

  static func nowPlayingInfo(_ payload: VlcNowPlaying) -> NowPlayingInfo {
    NowPlayingInfo(title: payload.title, artist: payload.artist, album: payload.album, artworkUri: payload.artworkUri)
  }

  static func subtitleStyle(_ style: SubtitleStyle) -> VlcSubtitleStyle {
    VlcSubtitleStyle(
      fontFamily: style.fontFamily,
      fontScale: style.fontScale.map(Double.init),
      bold: style.bold,
      color: style.color?.hex,
      backgroundColor: style.backgroundColor?.hex,
      outlineColor: style.outlineColor?.hex,
      outlineThickness: style.outlineThickness.map(outlineThickness),
      shadowColor: style.shadowColor?.hex,
      shadowDistance: style.shadowDistance,
      marginBottom: style.marginBottom.map(Double.init)
    )
  }

  static func subtitleStyle(_ payload: VlcSubtitleStyle) -> SubtitleStyle? {
    SubtitleStyle(
      fontFamily: VlcContract.fontFamily(payload.fontFamily),
      fontScale: payload.fontScale.flatMap(VlcContract.clampFontScale),
      bold: payload.bold,
      color: payload.color.flatMap(SubtitleColor.init(hex:)),
      backgroundColor: payload.backgroundColor.flatMap(SubtitleColor.init(hex:)),
      outlineColor: payload.outlineColor.flatMap(SubtitleColor.init(hex:)),
      outlineThickness: payload.outlineThickness.map(subtitleOutline),
      shadowColor: payload.shadowColor.flatMap(SubtitleColor.init(hex:)),
      shadowDistance: payload.shadowDistance.flatMap(VlcContract.clampShadowDistance),
      marginBottom: payload.marginBottom.flatMap(VlcContract.clampSubtitleMargin)
    ).nonEmpty
  }

  private static func outlineThickness(_ outline: SubtitleOutline) -> VlcOutlineThickness {
    switch outline {
    case .none: return .none
    case .thin: return .thin
    case .normal: return .normal
    case .thick: return .thick
    }
  }

  private static func subtitleOutline(_ thickness: VlcOutlineThickness) -> SubtitleOutline {
    switch thickness {
    case .none: return .none
    case .thin: return .thin
    case .normal: return .normal
    case .thick: return .thick
    }
  }
}
