import AVFoundation
import XCTest
@testable import RnVlcPlyr

final class AudioSessionObserverTests: XCTestCase {
  func testInterruptionBeganIsParsed() {
    let userInfo: [AnyHashable: Any] = [AVAudioSessionInterruptionTypeKey: AVAudioSession.InterruptionType.began.rawValue]
    XCTAssertEqual(AudioSessionObserver.interruptionEvent(userInfo), .interruptionBegan)
  }

  func testInterruptionEndedCarriesTheResumeOption() {
    let ended = AVAudioSession.InterruptionType.ended.rawValue
    let resuming: [AnyHashable: Any] = [
      AVAudioSessionInterruptionTypeKey: ended,
      AVAudioSessionInterruptionOptionKey: AVAudioSession.InterruptionOptions.shouldResume.rawValue,
    ]
    XCTAssertEqual(AudioSessionObserver.interruptionEvent(resuming), .interruptionEnded(shouldResume: true))
    XCTAssertEqual(AudioSessionObserver.interruptionEvent([AVAudioSessionInterruptionTypeKey: ended]), .interruptionEnded(shouldResume: false))
  }

  func testAnInterruptionWithoutAKnownTypeIsIgnored() {
    XCTAssertNil(AudioSessionObserver.interruptionEvent(nil))
    XCTAssertNil(AudioSessionObserver.interruptionEvent([AVAudioSessionInterruptionTypeKey: UInt.max]))
  }

  func testOnlyAnUnavailableOldDeviceIsARouteEvent() {
    let unplugged: [AnyHashable: Any] = [AVAudioSessionRouteChangeReasonKey: AVAudioSession.RouteChangeReason.oldDeviceUnavailable.rawValue]
    let pluggedIn: [AnyHashable: Any] = [AVAudioSessionRouteChangeReasonKey: AVAudioSession.RouteChangeReason.newDeviceAvailable.rawValue]
    XCTAssertEqual(AudioSessionObserver.routeChangeEvent(unplugged), .oldDeviceUnavailable)
    XCTAssertNil(AudioSessionObserver.routeChangeEvent(pluggedIn))
    XCTAssertNil(AudioSessionObserver.routeChangeEvent(nil))
  }
}

final class MediaSessionCoordinatorTests: XCTestCase {
  private let store = PlayerStore()
  private let control = RecordingSessionControl()
  private var coordinator: MediaSessionCoordinator!

  override func setUp() {
    super.setUp()
    coordinator = MediaSessionCoordinator(store: store, control: control)
  }

  override func tearDown() {
    coordinator.shutdown()
    coordinator = nil
    super.tearDown()
  }

  private func setStatus(_ status: PlayerStatus) {
    store.updateMedia { $0.status = status }
  }

  private func postInterruption(_ userInfo: [AnyHashable: Any]) {
    NotificationCenter.default.post(name: AVAudioSession.interruptionNotification, object: AVAudioSession.sharedInstance(), userInfo: userInfo)
  }

  func testToggleFollowsTheStatus() {
    setStatus(.playing)
    coordinator.handle(.togglePlayPause)
    setStatus(.buffering)
    coordinator.handle(.togglePlayPause)
    setStatus(.paused)
    coordinator.handle(.togglePlayPause)

    XCTAssertEqual(control.calls, ["pause", "pause", "play"])
  }

  func testPlayPauseAndSeekGoToTheControl() {
    coordinator.handle(.play)
    coordinator.handle(.pause)
    coordinator.handle(.seek(toMs: 4000))

    XCTAssertEqual(control.calls, ["play", "pause", "seekTo:4000"])
  }

  func testSkipUsesTheRemoteSkipInterval() {
    _ = store.updateSettings { $0.remoteSkipInterval = 15_000 }

    coordinator.handle(.skipForward)
    coordinator.handle(.skipBackward)

    XCTAssertEqual(control.calls, ["seekBy:15000", "seekBy:-15000"])
  }

  func testNextAndPreviousDoNotReachTheControl() {
    coordinator.handle(.next)
    coordinator.handle(.previous)

    XCTAssertEqual(control.calls, [])
  }

  func testAnInterruptionPausesAnActivePlayerAndItsEndResumesIt() {
    setStatus(.playing)

    postInterruption([AVAudioSessionInterruptionTypeKey: AVAudioSession.InterruptionType.began.rawValue])
    postInterruption([
      AVAudioSessionInterruptionTypeKey: AVAudioSession.InterruptionType.ended.rawValue,
      AVAudioSessionInterruptionOptionKey: AVAudioSession.InterruptionOptions.shouldResume.rawValue,
    ])

    XCTAssertEqual(control.calls, ["pauseForSession", "resumeForSession"])
  }

  func testAnInterruptionOfAnIdlePlayerDoesNothing() {
    postInterruption([AVAudioSessionInterruptionTypeKey: AVAudioSession.InterruptionType.began.rawValue])

    XCTAssertEqual(control.calls, [])
  }
}

private final class RecordingSessionControl: MediaSessionControl {
  private(set) var calls: [String] = []

  func play() {
    calls.append("play")
  }

  func pause() {
    calls.append("pause")
  }

  func seek(toMs target: Double) {
    calls.append("seekTo:\(Int(target))")
  }

  func seek(byMs delta: Double) {
    calls.append("seekBy:\(Int(delta))")
  }

  func pauseForSession() {
    calls.append("pauseForSession")
  }

  func resumeForSession() {
    calls.append("resumeForSession")
  }

  func setVideoTrackEnabled(_ enabled: Bool) {
    calls.append("video:\(enabled)")
  }
}
