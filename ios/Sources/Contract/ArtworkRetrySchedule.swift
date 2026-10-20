import Foundation

struct ArtworkRetrySchedule {
  static let delaysMs = [0, 1000, 3000, 9000]

  private(set) var attempts = 0

  mutating func nextDelayMs() -> Int? {
    guard attempts < Self.delaysMs.count else { return nil }
    defer { attempts += 1 }
    return Self.delaysMs[attempts]
  }

  mutating func reset() {
    attempts = 0
  }
}
