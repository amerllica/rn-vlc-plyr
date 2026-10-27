import XCTest
@testable import RnVlcPlyr

final class ResumeReconnectTests: XCTestCase {
  private let networkVod = ResumeTarget(isNetworkSource: true, isLive: false, isSeekable: true)

  func testReconnectsAfterTheLimit() {
    var reconnect = ResumeReconnect()
    reconnect.didPause(atMs: 1000)
    XCTAssertTrue(reconnect.takeReconnect(atMs: 31_000, afterMs: 30_000, target: networkVod))
  }

  func testDoesNotReconnectBeforeTheLimit() {
    var reconnect = ResumeReconnect()
    reconnect.didPause(atMs: 1000)
    XCTAssertFalse(reconnect.takeReconnect(atMs: 30_999, afterMs: 30_000, target: networkVod))
  }

  func testZeroLimitTurnsItOff() {
    var reconnect = ResumeReconnect()
    reconnect.didPause(atMs: 0)
    XCTAssertFalse(reconnect.takeReconnect(atMs: 1_000_000, afterMs: 0, target: networkVod))
  }

  func testSkipsFilesLiveStreamsAndUnseekableMedia() {
    let targets = [
      ResumeTarget(isNetworkSource: false, isLive: false, isSeekable: true),
      ResumeTarget(isNetworkSource: true, isLive: true, isSeekable: false),
      ResumeTarget(isNetworkSource: true, isLive: false, isSeekable: false),
    ]
    for target in targets {
      var reconnect = ResumeReconnect()
      reconnect.didPause(atMs: 0)
      XCTAssertFalse(reconnect.takeReconnect(atMs: 60_000, afterMs: 1000, target: target))
    }
  }

  func testNeedsARecordedPauseAndConsumesIt() {
    var reconnect = ResumeReconnect()
    XCTAssertFalse(reconnect.takeReconnect(atMs: 60_000, afterMs: 1000, target: networkVod))
    reconnect.didPause(atMs: 0)
    XCTAssertTrue(reconnect.takeReconnect(atMs: 60_000, afterMs: 1000, target: networkVod))
    XCTAssertFalse(reconnect.takeReconnect(atMs: 120_000, afterMs: 1000, target: networkVod))
  }

  func testClearForgetsThePause() {
    var reconnect = ResumeReconnect()
    reconnect.didPause(atMs: 0)
    reconnect.clear()
    XCTAssertFalse(reconnect.takeReconnect(atMs: 60_000, afterMs: 1000, target: networkVod))
  }
}
