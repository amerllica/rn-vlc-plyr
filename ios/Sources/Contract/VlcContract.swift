import Foundation

enum VlcContract {
  static let volumeRange = 0...100
  static let rateRange = 0.25...4.0
  static let timeUpdateIntervalRange = 50...5000
  static let delayRange = -60_000...60_000
  static let skipIntervalRange = 1000...60_000
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

  static func clampSkipInterval(_ value: Double) -> Int? {
    roundedClamp(value, to: skipIntervalRange)
  }

  static func clampRecoveryDelay(_ value: Double) -> Int {
    guard value.isFinite, value > 0 else { return 0 }
    return Int(min(jsRound(value), Double(Int32.max)))
  }

  static func libvlcOptions(_ values: [String]) -> [String] {
    values
      .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
      .filter { !$0.isEmpty }
  }

  static func instanceOptions(libvlcOptions: [String], subtitleStyle: SubtitleStyle?) -> [String] {
    libvlcOptions + subtitleStyleOptions(subtitleStyle)
  }

  static func bufferingPercent(fromEngineCache cache: Float) -> Int? {
    guard cache.isFinite else { return nil }
    return Int(min(max(jsRound(Double(cache)), 0), 100))
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

  static func roundedClamp(_ value: Double, to range: ClosedRange<Int>) -> Int? {
    guard !value.isNaN else { return nil }
    let clamped = min(max(jsRound(value), Double(range.lowerBound)), Double(range.upperBound))
    return Int(clamped)
  }

  static func jsRound(_ value: Double) -> Double {
    (value + 0.5).rounded(.down)
  }
}
