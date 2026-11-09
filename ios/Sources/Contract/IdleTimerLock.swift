import Foundation

struct IdleTimerLock {
  private var holders = Set<ObjectIdentifier>()
  private var ownsDisable = false

  mutating func update(_ holder: ObjectIdentifier, keepsAwake: Bool, isDisabled: Bool) -> Bool? {
    if keepsAwake {
      holders.insert(holder)
    } else {
      holders.remove(holder)
    }
    if !holders.isEmpty, !isDisabled {
      ownsDisable = true
      return true
    }
    if holders.isEmpty, ownsDisable {
      ownsDisable = false
      return false
    }
    return nil
  }
}
