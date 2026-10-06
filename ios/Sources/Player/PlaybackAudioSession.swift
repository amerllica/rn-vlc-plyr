import AVFoundation

struct PlaybackAudioSession {
  private var isConfigured = false

  mutating func activateOnce() {
    guard !isConfigured else { return }
    isConfigured = true
    let session = AVAudioSession.sharedInstance()
    try? session.setCategory(.playback, mode: .moviePlayback)
    try? session.setActive(true)
  }
}
