import Foundation

enum VlcContract {
  static let volumeRange = 0...100
  static let rateRange = 0.25...4.0
  static let timeUpdateIntervalRange = 50...5000
  static let delayRange = -60_000...60_000
  static let microsecondsPerMillisecond = 1000

  static func clampVolume(_ value: Double) -> Int? {
    roundedClamp(value, to: volumeRange)
  }

  static func clampRate(_ value: Double) -> Double? {
    guard !value.isNaN else { return nil }
    return min(max(value, rateRange.lowerBound), rateRange.upperBound)
  }

  static func clampTimeUpdateInterval(_ value: Double) -> Int? {
    roundedClamp(value, to: timeUpdateIntervalRange)
  }

  static func clampDelay(_ value: Double) -> Int? {
    roundedClamp(value, to: delayRange)
  }

  static func engineDelay(fromMilliseconds milliseconds: Int) -> Int {
    milliseconds * microsecondsPerMillisecond
  }

  static func clampSeekTarget(_ value: Double, duration: Int) -> Int? {
    guard !value.isNaN else { return nil }
    let upperBound = duration > 0 ? Double(duration) : Double(Int.max / 2)
    return Int(min(max(jsRound(value), 0), upperBound))
  }

  static func duration(fromEngineLength length: Int) -> Int {
    max(length, 0)
  }

  static func isLive(duration: Int, isSeekable: Bool, hasReachedPlaying: Bool) -> Bool {
    hasReachedPlaying && duration == 0 && !isSeekable
  }

  private static func roundedClamp(_ value: Double, to range: ClosedRange<Int>) -> Int? {
    guard !value.isNaN else { return nil }
    let clamped = min(max(jsRound(value), Double(range.lowerBound)), Double(range.upperBound))
    return Int(clamped)
  }

  static func jsRound(_ value: Double) -> Double {
    (value + 0.5).rounded(.down)
  }
}
