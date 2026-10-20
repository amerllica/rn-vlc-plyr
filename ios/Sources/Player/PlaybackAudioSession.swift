import AVFoundation

struct PlaybackAudioSession {
  private static var activeClients = 0

  private var isActive = false

  mutating func activate() {
    let session = AVAudioSession.sharedInstance()
    if !isActive {
      isActive = true
      Self.activeClients += 1
      try? session.setCategory(.playback, mode: .moviePlayback)
    }
    try? session.setActive(true)
  }

  mutating func deactivate() {
    guard isActive else { return }
    isActive = false
    Self.activeClients -= 1
    guard Self.activeClients == 0 else { return }
    try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
  }
}
