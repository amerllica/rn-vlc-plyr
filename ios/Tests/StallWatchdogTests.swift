import XCTest
@testable import RnVlcPlyr

final class StallWatchdogTests: XCTestCase {
  private static let positionMs = 42_000
  private static let seekTargetMs = 10_000
  private static let progressMs = 4000

  private var watchdog = StallWatchdog()

  func testFiresWhileStillBuffering() {
    let generation = watchdog.arm(positionMs: Self.positionMs)
    XCTAssertTrue(watchdog.fire(generation: generation, status: .buffering, positionMs: Self.positionMs + Self.progressMs))
  }

  func testFiresWhenPlayingWithoutProgress() {
    let generation = watchdog.arm(positionMs: Self.positionMs)
    XCTAssertTrue(watchdog.fire(generation: generation, status: .playing, positionMs: Self.positionMs))
  }

  func testStaysQuietOncePlaybackMoves() {
    let generation = watchdog.arm(positionMs: Self.positionMs)
    XCTAssertFalse(watchdog.fire(generation: generation, status: .playing, positionMs: Self.positionMs + 1))
  }

  func testFiresOncePerResume() {
    let generation = watchdog.arm(positionMs: Self.positionMs)
    XCTAssertTrue(watchdog.fire(generation: generation, status: .buffering, positionMs: Self.positionMs))
    XCTAssertFalse(watchdog.isArmed)
    XCTAssertFalse(watchdog.fire(generation: generation, status: .buffering, positionMs: Self.positionMs))
  }

  func testDisarmedOrOutdatedChecksDoNothing() {
    let first = watchdog.arm(positionMs: Self.positionMs)
    let second = watchdog.arm(positionMs: Self.positionMs)

    XCTAssertFalse(watchdog.fire(generation: first, status: .buffering, positionMs: Self.positionMs))

    watchdog.disarm()

    XCTAssertFalse(watchdog.fire(generation: second, status: .buffering, positionMs: Self.positionMs))
  }

  func testArmingAgainAfterASeekMeasuresFromTheSeekTarget() {
    let beforeSeek = watchdog.arm(positionMs: Self.positionMs)
    XCTAssertTrue(watchdog.isArmed)
    let afterSeek = watchdog.arm(positionMs: Self.seekTargetMs)

    XCTAssertFalse(watchdog.fire(generation: beforeSeek, status: .playing, positionMs: Self.seekTargetMs + Self.progressMs))
    XCTAssertFalse(watchdog.fire(generation: afterSeek, status: .playing, positionMs: Self.seekTargetMs + Self.progressMs))
    XCTAssertFalse(watchdog.isArmed)
  }

  func testOtherStatusesDoNotFire() {
    for status in [PlayerStatus.paused, .opening, .stopped, .ended, .error] {
      let generation = watchdog.arm(positionMs: Self.positionMs)
      XCTAssertFalse(watchdog.fire(generation: generation, status: status, positionMs: Self.positionMs), "\(status)")
    }
  }
}
