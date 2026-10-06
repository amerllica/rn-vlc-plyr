import CoreGraphics

struct ResizeSettings: Equatable {
  static let fit = ResizeSettings(scaleFactor: 0, aspectRatio: nil, cropGeometry: nil)

  let scaleFactor: Float
  let aspectRatio: String?
  let cropGeometry: String?
}

enum ResizeMode {
  case contain
  case cover
  case stretch
  case original
}

enum ResizeGeometry {
  static let nativePixelScale: Float = 1

  static func settings(mode: ResizeMode, viewSize: CGSize) -> ResizeSettings {
    switch mode {
    case .contain:
      return .fit
    case .original:
      return ResizeSettings(scaleFactor: nativePixelScale, aspectRatio: nil, cropGeometry: nil)
    case .stretch:
      return ResizeSettings(scaleFactor: 0, aspectRatio: ratio(of: viewSize), cropGeometry: nil)
    case .cover:
      return ResizeSettings(scaleFactor: 0, aspectRatio: nil, cropGeometry: ratio(of: viewSize))
    }
  }

  static func ratio(of size: CGSize) -> String? {
    let width = Int(size.width.rounded())
    let height = Int(size.height.rounded())
    guard width > 0, height > 0 else { return nil }
    let divisor = greatestCommonDivisor(width, height)
    return "\(width / divisor):\(height / divisor)"
  }

  private static func greatestCommonDivisor(_ a: Int, _ b: Int) -> Int {
    b == 0 ? a : greatestCommonDivisor(b, a % b)
  }
}
