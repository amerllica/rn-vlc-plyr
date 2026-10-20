import UIKit

enum AppLifecycleObserver {
  static func observe(onBackground: @escaping () -> Void, onForeground: @escaping () -> Void) -> NotificationObservers {
    NotificationObservers([
      UIApplication.didEnterBackgroundNotification: { _ in onBackground() },
      UIApplication.willEnterForegroundNotification: { _ in onForeground() },
    ])
  }
}
