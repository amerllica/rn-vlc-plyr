import ImageIO
import UIKit

enum ArtworkFailure: Error, Equatable {
  case unsupportedScheme
  case fileNotFound
  case notAnImage
  case timedOut
  case http(Int)
  case transport(String)

  init(_ error: Error) {
    if (error as? URLError)?.code == .timedOut {
      self = .timedOut
    } else {
      self = .transport(error.localizedDescription)
    }
  }

  var reason: String {
    switch self {
    case .unsupportedScheme: return "unsupported scheme"
    case .fileNotFound: return "file not found"
    case .notAnImage: return "not an image"
    case .timedOut: return "timed out"
    case .http(let status): return "HTTP \(status)"
    case .transport(let description): return description
    }
  }
}

final class ArtworkLoader {
  typealias Scheduler = (_ delayMs: Int, _ work: @escaping () -> Void) -> Void
  typealias Fetcher = (_ uri: String, _ completion: @escaping (Result<UIImage, ArtworkFailure>) -> Void) -> Void

  static let maxPixelSize = 512
  static let timeoutSeconds: TimeInterval = 10
  private static let remoteSchemes: Set<String> = ["http", "https"]
  private static let successStatuses = 200..<300
  private static let decodeQueue = DispatchQueue(label: "rn-vlc-plyr.artwork", qos: .utility)

  static let mainQueueScheduler: Scheduler = { delayMs, work in
    DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(delayMs), execute: work)
  }

  static let systemLog: (String) -> Void = { message in
    NSLog("%@", message)
  }

  private struct Load: Equatable {
    let uri: String
    let cycle: Int
  }

  private let onLoad: () -> Void
  private let schedule: Scheduler
  private let fetch: Fetcher
  private let log: (String) -> Void
  private var cached: (uri: String, image: UIImage)?
  private var failed: Set<String> = []
  private var current: Load?
  private var cycles = 0
  private var retry = ArtworkRetrySchedule()

  init(
    schedule: @escaping Scheduler = ArtworkLoader.mainQueueScheduler,
    fetch: @escaping Fetcher = ArtworkLoader.fetch,
    log: @escaping (String) -> Void = ArtworkLoader.systemLog,
    onLoad: @escaping () -> Void
  ) {
    self.schedule = schedule
    self.fetch = fetch
    self.log = log
    self.onLoad = onLoad
  }

  func image(for uri: String) -> UIImage? {
    if let cached, cached.uri == uri {
      return cached.image
    }
    if current?.uri != uri, !failed.contains(uri) {
      start(uri)
    }
    return nil
  }

  func reset(_ uri: String) {
    failed.remove(uri)
    if current?.uri == uri {
      retry.reset()
    }
  }

  private func start(_ uri: String) {
    cycles += 1
    let load = Load(uri: uri, cycle: cycles)
    current = load
    retry.reset()
    attempt(load, afterMs: retry.nextDelayMs() ?? 0)
  }

  private func attempt(_ load: Load, afterMs delayMs: Int) {
    guard delayMs > 0 else {
      attempt(load)
      return
    }
    schedule(delayMs) { [weak self] in
      guard let self, self.current == load else { return }
      self.attempt(load)
    }
  }

  private func attempt(_ load: Load) {
    fetch(load.uri) { [weak self] result in
      DispatchQueue.main.async {
        self?.finish(load, result)
      }
    }
  }

  private func finish(_ load: Load, _ result: Result<UIImage, ArtworkFailure>) {
    guard current == load else { return }
    switch result {
    case .success(let image):
      current = nil
      cached = (load.uri, image)
      onLoad()
    case .failure(let failure):
      retryOrGiveUp(load, failure)
    }
  }

  private func retryOrGiveUp(_ load: Load, _ failure: ArtworkFailure) {
    guard let delayMs = retry.nextDelayMs() else {
      giveUp(load, failure)
      return
    }
    attempt(load, afterMs: delayMs)
  }

  private func giveUp(_ load: Load, _ failure: ArtworkFailure) {
    current = nil
    failed.insert(load.uri)
    log("rn-vlc-plyr: artwork could not be loaded from \(load.uri): \(failure.reason)")
    onLoad()
  }

  static func fetch(_ uri: String, completion: @escaping (Result<UIImage, ArtworkFailure>) -> Void) {
    guard let url = URL(string: uri), let scheme = url.scheme?.lowercased() else {
      completion(.failure(.unsupportedScheme))
      return
    }
    if url.isFileURL {
      decodeQueue.async { completion(readFile(url)) }
    } else if remoteSchemes.contains(scheme) {
      download(url, completion: completion)
    } else {
      completion(.failure(.unsupportedScheme))
    }
  }

  private static func readFile(_ url: URL) -> Result<UIImage, ArtworkFailure> {
    guard let data = try? Data(contentsOf: url) else { return .failure(.fileNotFound) }
    return decode(data)
  }

  private static func download(_ url: URL, completion: @escaping (Result<UIImage, ArtworkFailure>) -> Void) {
    var request = URLRequest(url: url)
    request.timeoutInterval = timeoutSeconds
    URLSession.shared.dataTask(with: request) { data, response, error in
      completion(downloadResult(data, response, error))
    }.resume()
  }

  private static func downloadResult(_ data: Data?, _ response: URLResponse?, _ error: Error?) -> Result<UIImage, ArtworkFailure> {
    if let error {
      return .failure(ArtworkFailure(error))
    }
    if let status = (response as? HTTPURLResponse)?.statusCode, !successStatuses.contains(status) {
      return .failure(.http(status))
    }
    guard let data else { return .failure(.notAnImage) }
    return decode(data)
  }

  private static func decode(_ data: Data) -> Result<UIImage, ArtworkFailure> {
    guard let image = downscaledImage(from: data) else { return .failure(.notAnImage) }
    return .success(image)
  }

  static func downscaledImage(from data: Data) -> UIImage? {
    guard let source = CGImageSourceCreateWithData(data as CFData, nil) else { return nil }
    let options: [CFString: Any] = [
      kCGImageSourceCreateThumbnailFromImageAlways: true,
      kCGImageSourceCreateThumbnailWithTransform: true,
      kCGImageSourceShouldCacheImmediately: true,
      kCGImageSourceThumbnailMaxPixelSize: maxPixelSize,
    ]
    guard let thumbnail = CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary) else { return nil }
    return UIImage(cgImage: thumbnail)
  }
}
