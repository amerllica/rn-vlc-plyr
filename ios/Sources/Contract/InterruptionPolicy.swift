import Foundation

enum InterruptionEffect: Equatable {
  case pause
  case resume
}

struct InterruptionPolicy {
  private(set) var pausedByInterruption = false

  mutating func began(isActive: Bool) -> InterruptionEffect? {
    guard isActive else { return nil }
    pausedByInterruption = true
    return .pause
  }

  mutating func ended(shouldResume: Bool) -> InterruptionEffect? {
    let resumes = pausedByInterruption && shouldResume
    pausedByInterruption = false
    return resumes ? .resume : nil
  }

  mutating func lostPermanently(isActive: Bool) -> InterruptionEffect? {
    pauseWithoutResuming(isActive: isActive)
  }

  mutating func becameNoisy(isActive: Bool) -> InterruptionEffect? {
    pauseWithoutResuming(isActive: isActive)
  }

  mutating func reset() {
    pausedByInterruption = false
  }

  private mutating func pauseWithoutResuming(isActive: Bool) -> InterruptionEffect? {
    reset()
    return isActive ? .pause : nil
  }
}
