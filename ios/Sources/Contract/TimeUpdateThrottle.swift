import Foundation

struct TimeUpdateThrottle {
  private var lastEmissionMs: Double?

  mutating func shouldEmit(atMs nowMs: Double, intervalMs: Int) -> Bool {
    if let lastEmissionMs, nowMs - lastEmissionMs < Double(intervalMs) {
      return false
    }
    lastEmissionMs = nowMs
    return true
  }

  mutating func markEmitted(atMs nowMs: Double) {
    lastEmissionMs = nowMs
  }

  mutating func reset() {
    lastEmissionMs = nil
  }
}
