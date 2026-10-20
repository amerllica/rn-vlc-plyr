import ImageIO
import UIKit

final class ArtworkLoader {
  static let maxPixelSize = 512
  private static let remoteSchemes: Set<String> = ["http", "https"]
  private static let decodeQueue = DispatchQueue(label: "rn-vlc-plyr.artwork", qos: .utility)

  private let onLoad: () -> Void
  private var cached: (uri: String, image: UIImage?)?
  private var loading: Set<String> = []

  init(onLoad: @escaping () -> Void) {
    self.onLoad = onLoad
  }

  func image(for uri: String) -> UIImage? {
    if let cached, cached.uri == uri {
      return cached.image
    }
    if !loading.contains(uri) {
      loading.insert(uri)
      fetch(uri)
    }
    return nil
  }

  private func fetch(_ uri: String) {
    let deliver: (Data?) -> Void = { [weak self] data in
      let image = data.flatMap(Self.downscaledImage)
      DispatchQueue.main.async {
        self?.finish(uri, image)
      }
    }
    guard let url = URL(string: uri) else {
      deliver(nil)
      return
    }
    if url.isFileURL {
      Self.decodeQueue.async { deliver(try? Data(contentsOf: url)) }
    } else if Self.remoteSchemes.contains(url.scheme?.lowercased() ?? "") {
      URLSession.shared.dataTask(with: url) { data, _, _ in deliver(data) }.resume()
    } else {
      deliver(nil)
    }
  }

  private func finish(_ uri: String, _ image: UIImage?) {
    loading.remove(uri)
    cached = (uri, image)
    onLoad()
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
