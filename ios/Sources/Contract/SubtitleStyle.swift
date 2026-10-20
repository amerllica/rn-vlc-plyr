import Foundation

struct SubtitleColor: Equatable {
  static let opaque = 255
  private static let prefix = "#"
  private static let shortLength = 3
  private static let rgbLength = 6
  private static let rgbaLength = 8
  private static let alphaBits = 8

  let rgb: Int
  let alpha: Int

  init(rgb: Int, alpha: Int = SubtitleColor.opaque) {
    self.rgb = rgb
    self.alpha = alpha
  }

  init?(hex: String) {
    let trimmed = hex.trimmingCharacters(in: .whitespacesAndNewlines)
    guard trimmed.hasPrefix(Self.prefix) else { return nil }
    let digits = String(trimmed.dropFirst())
    let expanded = digits.count == Self.shortLength ? digits.map { "\($0)\($0)" }.joined() : digits
    guard expanded.count == Self.rgbLength || expanded.count == Self.rgbaLength,
          expanded.unicodeScalars.allSatisfy({ $0.isASCII && $0.properties.isASCIIHexDigit }),
          let value = Int(expanded, radix: 16) else { return nil }
    if expanded.count == Self.rgbaLength {
      self.init(rgb: value >> Self.alphaBits, alpha: value & Self.opaque)
    } else {
      self.init(rgb: value)
    }
  }

  var hex: String {
    alpha == Self.opaque ? String(format: "#%06X", rgb) : String(format: "#%06X%02X", rgb, alpha)
  }
}

enum SubtitleOutline: Int, Equatable {
  case none = 0
  case thin = 2
  case normal = 4
  case thick = 6
}

struct SubtitleStyle: Equatable {
  var fontFamily: String?
  var fontScale: Int?
  var bold: Bool?
  var color: SubtitleColor?
  var backgroundColor: SubtitleColor?
  var outlineColor: SubtitleColor?
  var outlineThickness: SubtitleOutline?
  var shadowColor: SubtitleColor?
  var shadowDistance: Double?
  var marginBottom: Int?

  var nonEmpty: SubtitleStyle? {
    self == SubtitleStyle() ? nil : self
  }
}

extension VlcContract {
  static let fontScaleRange = 25...400
  static let subtitleMarginRange = 0...1000
  static let shadowDistanceRange = 0.0...1.0
  static let defaultRelativeFontSize = 16
  static let fullFontScale = 100

  static func fontFamily(_ value: String?) -> String? {
    guard let trimmed = value?.trimmingCharacters(in: .whitespacesAndNewlines), !trimmed.isEmpty else { return nil }
    return trimmed
  }

  static func clampFontScale(_ value: Double) -> Int? {
    roundedClamp(value, to: fontScaleRange)
  }

  static func clampSubtitleMargin(_ value: Double) -> Int? {
    roundedClamp(value, to: subtitleMarginRange)
  }

  static func clampShadowDistance(_ value: Double) -> Double? {
    guard !value.isNaN else { return nil }
    return min(max(value, shadowDistanceRange.lowerBound), shadowDistanceRange.upperBound)
  }

  static func relativeFontSize(forScale scale: Int) -> Int {
    Int(jsRound(Double(defaultRelativeFontSize * fullFontScale) / Double(scale)))
  }

  static func subtitleStyleOptions(_ style: SubtitleStyle?) -> [String] {
    guard let style else { return [] }
    var options: [String] = []
    if let fontFamily = style.fontFamily {
      options.append("--freetype-font=\(fontFamily)")
    }
    if let fontScale = style.fontScale {
      options.append("--freetype-rel-fontsize=\(relativeFontSize(forScale: fontScale))")
    }
    if style.bold == true {
      options.append("--freetype-bold")
    }
    options += colorOptions(style.color, color: "freetype-color", opacity: "freetype-opacity")
    options += colorOptions(style.backgroundColor, color: "freetype-background-color", opacity: "freetype-background-opacity")
    options += colorOptions(style.outlineColor, color: "freetype-outline-color", opacity: "freetype-outline-opacity")
    if let outlineThickness = style.outlineThickness {
      options.append("--freetype-outline-thickness=\(outlineThickness.rawValue)")
    }
    options += colorOptions(style.shadowColor, color: "freetype-shadow-color", opacity: "freetype-shadow-opacity")
    if let shadowDistance = style.shadowDistance {
      options.append("--freetype-shadow-distance=\(shadowDistance)")
    }
    if let marginBottom = style.marginBottom {
      options.append("--sub-margin=\(marginBottom)")
    }
    return options
  }

  private static func colorOptions(_ value: SubtitleColor?, color: String, opacity: String) -> [String] {
    guard let value else { return [] }
    return ["--\(color)=\(value.rgb)", "--\(opacity)=\(value.alpha)"]
  }
}
