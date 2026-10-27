import XCTest
@testable import RnVlcPlyr

final class StallWatchdogTests: XCTestCase {
  func testDueWhileStillBuffering() {
    var watchdog = StallWatchdog()
    watchdog.arm(positionMs: 5000)
    XCTAssertTrue(watchdog.isDue(status: .buffering, positionMs: 5000))
    XCTAssertTrue(watchdog.isDue(status: .buffering, positionMs: 5400))
  }

  func testDueWhilePlayingWithoutProgress() {
    var watchdog = StallWatchdog()
    watchdog.arm(positionMs: 5000)
    XCTAssertTrue(watchdog.isDue(status: .playing, positionMs: 5000))
    XCTAssertTrue(watchdog.isDue(status: .playing, positionMs: 4800))
  }

  func testNotDueOncePlaybackMoved() {
    var watchdog = StallWatchdog()
    watchdog.arm(positionMs: 5000)
    XCTAssertFalse(watchdog.isDue(status: .playing, positionMs: 5250))
  }

  func testNotDueOutsideBufferingAndPlaying() {
    for status in [PlayerStatus.idle, .opening, .paused, .stopped, .ended, .error] {
      var watchdog = StallWatchdog()
      watchdog.arm(positionMs: 0)
      XCTAssertFalse(watchdog.isDue(status: status, positionMs: 0), "\(status)")
      XCTAssertTrue(StallWatchdog.ends(at: status), "\(status)")
    }
  }

  func testPlayingAndBufferingKeepItArmed() {
    XCTAssertFalse(StallWatchdog.ends(at: .buffering))
    XCTAssertFalse(StallWatchdog.ends(at: .playing))
  }

  func testFiresOncePerArm() {
    var watchdog = StallWatchdog()
    watchdog.arm(positionMs: 0)
    XCTAssertTrue(watchdog.isDue(status: .buffering, positionMs: 0))
    watchdog.markFired()
    XCTAssertFalse(watchdog.isArmed)
    XCTAssertFalse(watchdog.isDue(status: .buffering, positionMs: 0))
    watchdog.arm(positionMs: 0)
    XCTAssertTrue(watchdog.isDue(status: .buffering, positionMs: 0))
  }

  func testNotDueWithoutArmOrAfterDisarm() {
    var watchdog = StallWatchdog()
    XCTAssertFalse(watchdog.isDue(status: .buffering, positionMs: 0))
    watchdog.arm(positionMs: 0)
    watchdog.disarm()
    XCTAssertFalse(watchdog.isDue(status: .buffering, positionMs: 0))
  }
}
