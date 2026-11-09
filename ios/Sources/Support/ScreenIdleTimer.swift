import UIKit

enum ScreenIdleTimer {
  private static let lock = Locked(IdleTimerLock())

  static func keepAwake(_ holder: AnyObject, _ keepsAwake: Bool) {
    let id = ObjectIdentifier(holder)
    MainThread.runNowOrAsync {
      let app = UIApplication.shared
      let next = lock.mutate { $0.update(id, keepsAwake: keepsAwake, isDisabled: app.isIdleTimerDisabled) }
      if let next {
        app.isIdleTimerDisabled = next
      }
    }
  }
}
