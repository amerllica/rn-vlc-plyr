import XCTest
@testable import RnVlcPlyr

final class VlcContractTests: XCTestCase {
  func testVolumeRoundsAndClamps() {
    XCTAssertEqual(VlcContract.clampVolume(-5), 0)
    XCTAssertEqual(VlcContract.clampVolume(150), 100)
    XCTAssertEqual(VlcContract.clampVolume(42.4), 42)
    XCTAssertEqual(VlcContract.clampVolume(42.5), 43)
    XCTAssertEqual(VlcContract.clampVolume(.infinity), 100)
    XCTAssertNil(VlcContract.clampVolume(.nan))
  }

  func testRateClamps() {
    XCTAssertEqual(VlcContract.clampRate(0.1), 0.25)
    XCTAssertEqual(VlcContract.clampRate(9), 4)
    XCTAssertEqual(VlcContract.clampRate(1.5), 1.5)
    XCTAssertNil(VlcContract.clampRate(.nan))
  }

  func testTimeUpdateIntervalRoundsAndClamps() {
    XCTAssertEqual(VlcContract.clampTimeUpdateInterval(10), 50)
    XCTAssertEqual(VlcContract.clampTimeUpdateInterval(10_000), 5000)
    XCTAssertEqual(VlcContract.clampTimeUpdateInterval(333.6), 334)
  }

  func testDelaysRoundClampAndConvertToMicroseconds() {
    XCTAssertEqual(VlcContract.clampDelay(-70_000), -60_000)
    XCTAssertEqual(VlcContract.clampDelay(70_000), 60_000)
    XCTAssertEqual(VlcContract.clampDelay(499.6), 500)
    XCTAssertEqual(VlcContract.clampDelay(-0.5), 0)
    XCTAssertEqual(VlcContract.clampDelay(-500.5), -500)
    XCTAssertEqual(VlcContract.clampDelay(-500.6), -501)
    XCTAssertEqual(VlcContract.engineDelay(fromMilliseconds: 500), 500_000)
    XCTAssertEqual(VlcContract.engineDelay(fromMilliseconds: -250), -250_000)
  }

  func testSeekTargetClampsToZeroAndDuration() {
    XCTAssertEqual(VlcContract.clampSeekTarget(-100, duration: 10_000), 0)
    XCTAssertEqual(VlcContract.clampSeekTarget(20_000, duration: 10_000), 10_000)
    XCTAssertEqual(VlcContract.clampSeekTarget(1234.6, duration: 10_000), 1235)
    XCTAssertEqual(VlcContract.clampSeekTarget(1234.5, duration: 10_000), 1235)
    XCTAssertEqual(VlcContract.clampSeekTarget(99_999, duration: 0), 99_999)
    XCTAssertNil(VlcContract.clampSeekTarget(.nan, duration: 0))
  }

  func testDurationFromEngineLength() {
    XCTAssertEqual(VlcContract.duration(fromEngineLength: -1), 0)
    XCTAssertEqual(VlcContract.duration(fromEngineLength: 0), 0)
    XCTAssertEqual(VlcContract.duration(fromEngineLength: 5000), 5000)
  }

  func testIsLiveOnlyAfterPlayingWithNoDurationAndNotSeekable() {
    XCTAssertFalse(VlcContract.isLive(duration: 0, isSeekable: false, hasReachedPlaying: false))
    XCTAssertTrue(VlcContract.isLive(duration: 0, isSeekable: false, hasReachedPlaying: true))
    XCTAssertFalse(VlcContract.isLive(duration: 0, isSeekable: true, hasReachedPlaying: true))
    XCTAssertFalse(VlcContract.isLive(duration: 1000, isSeekable: false, hasReachedPlaying: true))
  }
}
