import Foundation

struct StallWatchdog {
  private struct Arming {
    let generation: Int
    let positionMs: Int
  }

  private var armed: Arming?
  private var lastGeneration = 0

  var isArmed: Bool {
    armed != nil
  }

  mutating func arm(positionMs: Int) -> Int {
    lastGeneration += 1
    armed = Arming(generation: lastGeneration, positionMs: positionMs)
    return lastGeneration
  }

  mutating func disarm() {
    armed = nil
  }

  mutating func fire(generation: Int, status: PlayerStatus, positionMs: Int) -> Bool {
    guard let current = armed, current.generation == generation else { return false }
    armed = nil
    return status == .buffering || (status == .playing && positionMs <= current.positionMs)
  }
}
