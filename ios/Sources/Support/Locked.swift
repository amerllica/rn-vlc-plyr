import Foundation

final class Locked<Value> {
  private let lock = NSLock()
  private var value: Value

  init(_ value: Value) {
    self.value = value
  }

  func read<Result>(_ body: (Value) -> Result) -> Result {
    lock.lock()
    defer { lock.unlock() }
    return body(value)
  }

  @discardableResult
  func mutate<Result>(_ body: (inout Value) -> Result) -> Result {
    lock.lock()
    defer { lock.unlock() }
    return body(&value)
  }
}
