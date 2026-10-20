import AVFoundation

enum AudioSessionEvent: Equatable {
  case interruptionBegan
  case interruptionEnded(shouldResume: Bool)
  case oldDeviceUnavailable
}

enum AudioSessionObserver {
  static func observe(_ handler: @escaping (AudioSessionEvent) -> Void) -> NotificationObservers {
    NotificationObservers([
      AVAudioSession.interruptionNotification: { notification in
        if let event = interruptionEvent(notification.userInfo) { handler(event) }
      },
      AVAudioSession.routeChangeNotification: { notification in
        if let event = routeChangeEvent(notification.userInfo) { handler(event) }
      },
    ], object: AVAudioSession.sharedInstance())
  }

  static func interruptionEvent(_ userInfo: [AnyHashable: Any]?) -> AudioSessionEvent? {
    guard let rawType = userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt,
          let type = AVAudioSession.InterruptionType(rawValue: rawType) else { return nil }
    switch type {
    case .began:
      return .interruptionBegan
    case .ended:
      let rawOptions = userInfo?[AVAudioSessionInterruptionOptionKey] as? UInt ?? 0
      return .interruptionEnded(shouldResume: AVAudioSession.InterruptionOptions(rawValue: rawOptions).contains(.shouldResume))
    @unknown default:
      return nil
    }
  }

  static func routeChangeEvent(_ userInfo: [AnyHashable: Any]?) -> AudioSessionEvent? {
    guard let rawReason = userInfo?[AVAudioSessionRouteChangeReasonKey] as? UInt,
          AVAudioSession.RouteChangeReason(rawValue: rawReason) == .oldDeviceUnavailable else { return nil }
    return .oldDeviceUnavailable
  }
}
