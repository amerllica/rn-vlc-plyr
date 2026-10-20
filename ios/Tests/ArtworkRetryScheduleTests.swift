import XCTest
@testable import RnVlcPlyr

final class ArtworkRetryScheduleTests: XCTestCase {
  func testFirstAttemptHasNoDelay() {
    var schedule = ArtworkRetrySchedule()
    XCTAssertEqual(schedule.nextDelayMs(), 0)
    XCTAssertEqual(schedule.attempts, 1)
  }

  func testRetriesWithGrowingDelays() {
    var schedule = ArtworkRetrySchedule()
    let delays = (0..<4).map { _ in schedule.nextDelayMs() }
    XCTAssertEqual(delays, [0, 1000, 3000, 9000])
  }

  func testGivesUpAfterFourAttempts() {
    var schedule = ArtworkRetrySchedule()
    for _ in 0..<4 {
      XCTAssertNotNil(schedule.nextDelayMs())
    }
    XCTAssertNil(schedule.nextDelayMs())
    XCTAssertNil(schedule.nextDelayMs())
    XCTAssertEqual(schedule.attempts, 4)
  }

  func testResetStartsOver() {
    var schedule = ArtworkRetrySchedule()
    while schedule.nextDelayMs() != nil {}
    schedule.reset()
    XCTAssertEqual(schedule.attempts, 0)
    XCTAssertEqual(schedule.nextDelayMs(), 0)
    XCTAssertEqual(schedule.nextDelayMs(), 1000)
  }
}
