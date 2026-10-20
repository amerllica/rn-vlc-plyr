import Foundation

enum BackgroundAudioSetup {
  static let backgroundModesKey = "UIBackgroundModes"
  static let audioMode = "audio"
  static let missingSetupWarning = "rn-vlc-plyr: staysActiveInBackground needs \"audio\" in the UIBackgroundModes of Info.plist. See the README section \"Background audio and lock screen controls\"."

  private static var hasWarned = false

  static func hasAudioBackgroundMode(in bundle: Bundle = .main) -> Bool {
    let modes = bundle.object(forInfoDictionaryKey: backgroundModesKey) as? [String] ?? []
    return modes.contains(audioMode)
  }

  static func warnOnceIfMissing() {
    guard !hasWarned, !hasAudioBackgroundMode() else { return }
    hasWarned = true
    NSLog("%@", missingSetupWarning)
  }
}
