import Foundation

struct StallCheck: Equatable {
  let generation: Int
  let delayMs: Int
}

struct ResumePlan: Equatable {
  let reconnectAtMs: Int?
  let stallCheck: StallCheck?
}

struct ResumeGuard {
  private static let stallCheckEndingStatuses: Set<PlayerStatus> = [.idle, .opening, .paused, .stopped, .ended, .error]

  private var reconnect = ResumeReconnect()
  private var watchdog = StallWatchdog()

  mutating func resume(atMs nowMs: Double, positionMs: Int, settings: PlayerSettings, target: ResumeTarget) -> ResumePlan {
    let reconnects = reconnect.takeReconnect(atMs: nowMs, afterMs: settings.reconnectOnResumeAfterMs, target: target)
    return ResumePlan(
      reconnectAtMs: reconnects ? positionMs : nil,
      stallCheck: armStallCheck(positionMs: positionMs, delayMs: settings.reloadOnStallAfterMs)
    )
  }

  mutating func seek(toMs positionMs: Int, stallAfterMs: Int) -> StallCheck? {
    watchdog.isArmed ? armStallCheck(positionMs: positionMs, delayMs: stallAfterMs) : nil
  }

  mutating func statusChanged(_ status: PlayerStatus, atMs nowMs: Double) {
    if status == .paused {
      reconnect.didPause(atMs: nowMs)
    }
    if Self.stallCheckEndingStatuses.contains(status) {
      watchdog.disarm()
    }
  }

  mutating func isStalled(generation: Int, status: PlayerStatus, positionMs: Int) -> Bool {
    watchdog.fire(generation: generation, status: status, positionMs: positionMs)
  }

  mutating func reset() {
    reconnect.clear()
    watchdog.disarm()
  }

  private mutating func armStallCheck(positionMs: Int, delayMs: Int) -> StallCheck? {
    guard delayMs > 0 else {
      watchdog.disarm()
      return nil
    }
    return StallCheck(generation: watchdog.arm(positionMs: positionMs), delayMs: delayMs)
  }
}
