import Foundation

enum BackgroundEffect: Equatable {
  case pause
  case disableVideo
  case enableVideo
}

struct BackgroundPolicy {
  private(set) var inBackground = false
  private(set) var videoDisabled = false

  mutating func enterBackground(staysActive: Bool, isActive: Bool) -> [BackgroundEffect] {
    guard !inBackground else { return [] }
    inBackground = true
    return effectsWhileInBackground(staysActive: staysActive, isActive: isActive)
  }

  mutating func enterForeground() -> [BackgroundEffect] {
    inBackground = false
    guard videoDisabled else { return [] }
    videoDisabled = false
    return [.enableVideo]
  }

  mutating func staysActiveChanged(staysActive: Bool, isActive: Bool) -> [BackgroundEffect] {
    guard inBackground else { return [] }
    return effectsWhileInBackground(staysActive: staysActive, isActive: isActive)
  }

  private mutating func effectsWhileInBackground(staysActive: Bool, isActive: Bool) -> [BackgroundEffect] {
    guard staysActive else { return isActive ? [.pause] : [] }
    guard !videoDisabled else { return [] }
    videoDisabled = true
    return [.disableVideo]
  }
}
