import Foundation

struct StallWatchdog {
  private static let endingStatuses: Set<PlayerStatus> = [.idle, .opening, .paused, .stopped, .ended, .error]

  private var resumedAtPositionMs: Int?
  private var hasFired = false

  static func ends(at status: PlayerStatus) -> Bool {
    endingStatuses.contains(status)
  }

  var isArmed: Bool {
    resumedAtPositionMs != nil && !hasFired
  }

  mutating func arm(positionMs: Int) {
    resumedAtPositionMs = positionMs
    hasFired = false
  }

  mutating func disarm() {
    resumedAtPositionMs = nil
    hasFired = false
  }

  mutating func markFired() {
    hasFired = true
  }

  func isDue(status: PlayerStatus, positionMs: Int) -> Bool {
    guard isArmed, let resumedAtPositionMs else { return false }
    switch status {
    case .buffering: return true
    case .playing: return positionMs <= resumedAtPositionMs
    default: return false
    }
  }
}
