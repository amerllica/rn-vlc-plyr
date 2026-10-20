import Foundation

final class NotificationObservers {
  typealias Handler = (Notification) -> Void

  private let center = NotificationCenter.default
  private var tokens: [NSObjectProtocol] = []

  init(_ handlers: [Notification.Name: Handler], object: Any? = nil) {
    tokens = handlers.map { name, handler in
      center.addObserver(forName: name, object: object, queue: .main, using: handler)
    }
  }

  deinit {
    tokens.forEach(center.removeObserver)
  }
}
