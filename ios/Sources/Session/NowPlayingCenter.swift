import MediaPlayer

struct NowPlayingCenter {
  private static let millisecondsPerSecond = 1000.0

  private var center: MPNowPlayingInfoCenter { .default() }

  func apply(_ snapshot: MediaSessionSnapshot, artwork: UIImage?) {
    guard let info = snapshot.info else {
      clear()
      return
    }
    let state = snapshot.state
    var properties: [String: Any] = [
      MPMediaItemPropertyTitle: info.title,
      MPNowPlayingInfoPropertyElapsedPlaybackTime: Self.seconds(state.positionMs),
      MPNowPlayingInfoPropertyPlaybackRate: state.playbackRate,
      MPNowPlayingInfoPropertyDefaultPlaybackRate: snapshot.defaultRate,
      MPNowPlayingInfoPropertyIsLiveStream: snapshot.isLive,
    ]
    properties[MPMediaItemPropertyArtist] = info.artist
    properties[MPMediaItemPropertyAlbumTitle] = info.album
    if state.durationMs > 0 {
      properties[MPMediaItemPropertyPlaybackDuration] = Self.seconds(state.durationMs)
    }
    if let artwork {
      properties[MPMediaItemPropertyArtwork] = MPMediaItemArtwork(boundsSize: artwork.size) { _ in artwork }
    }
    center.nowPlayingInfo = properties
  }

  func clear() {
    center.nowPlayingInfo = nil
  }

  private static func seconds(_ milliseconds: Int) -> Double {
    Double(milliseconds) / millisecondsPerSecond
  }
}
