import Foundation

struct ResumeTarget: Equatable {
  let isNetworkSource: Bool
  let isLive: Bool
  let isSeekable: Bool

  var canReconnect: Bool {
    isNetworkSource && !isLive && isSeekable
  }
}

struct ResumeReconnect {
  private var pausedAtMs: Double?

  mutating func didPause(atMs nowMs: Double) {
    pausedAtMs = nowMs
  }

  mutating func clear() {
    pausedAtMs = nil
  }

  mutating func takeReconnect(atMs nowMs: Double, afterMs limitMs: Int, target: ResumeTarget) -> Bool {
    defer { pausedAtMs = nil }
    guard limitMs > 0, let pausedAtMs, target.canReconnect else { return false }
    return nowMs - pausedAtMs >= Double(limitMs)
  }
}
