import Foundation
import NitroModules

final class SnapshotRequests {
  static let timeoutSeconds: TimeInterval = 5

  private var pending: [String: Promise<String>] = [:]

  static func makeTemporaryPath() -> String {
    (NSTemporaryDirectory() as NSString).appendingPathComponent("vlc-snapshot-\(UUID().uuidString).png")
  }

  static func reject(_ promise: Promise<String>) {
    promise.reject(withError: RuntimeError.error(withMessage: PlayerErrors.noVideoFrame))
  }

  func track(path: String, promise: Promise<String>) {
    pending[path] = promise
    DispatchQueue.main.asyncAfter(deadline: .now() + Self.timeoutSeconds) { [weak self] in
      self?.finish(path: path)
    }
  }

  func engineDidFinishRequest(path: String, succeeded: Bool) {
    guard succeeded || Self.isWritten(path) else {
      pending.removeValue(forKey: path).map(Self.reject)
      return
    }
    if Self.isWritten(path) {
      finish(path: path)
    }
  }

  func engineDidTakeSnapshot() {
    pending.keys.filter(Self.isWritten).forEach(finish)
  }

  func rejectAll() {
    let promises = pending.values
    pending.removeAll()
    promises.forEach(Self.reject)
  }

  private func finish(path: String) {
    guard let promise = pending.removeValue(forKey: path) else { return }
    if Self.isWritten(path) {
      promise.resolve(withResult: URL(fileURLWithPath: path).absoluteString)
    } else {
      Self.reject(promise)
    }
  }

  private static func isWritten(_ path: String) -> Bool {
    let size = (try? FileManager.default.attributesOfItem(atPath: path)[.size] as? NSNumber)?.intValue ?? 0
    return size > 0
  }
}
