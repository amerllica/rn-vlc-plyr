import Foundation

struct PlayerSettings: Equatable {
  var volume = 100
  var muted = false
  var rate = 1.0
  var loop = false
  var autoPlay = true
  var timeUpdateInterval = 250
  var subtitleDelay = 0
  var audioDelay = 0

  var volumeState: VolumeState {
    VolumeState(volume: volume, muted: muted)
  }
}

struct MediaState {
  var status: PlayerStatus = .idle
  var error: PlayerFailure?
  var currentTime = 0
  var duration = 0
  var isLive = false
  var isSeekable = false
  var videoSize = VideoDimensions.zero
  var tracks = TracksSnapshot()
}
