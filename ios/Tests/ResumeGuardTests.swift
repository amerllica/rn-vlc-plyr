import XCTest
@testable import RnVlcPlyr

final class ResumeGuardTests: XCTestCase {
  private static let reconnectAfterMs = 30_000
  private static let stallAfterMs = 8000
  private static let longPauseMs = 60_000.0
  private static let shortPauseMs = 5000.0
  private static let positionMs = 60_000
  private static let seekTargetMs = 10_000
  private static let progressMs = 4000

  private let networkVideo = ResumeTarget(isNetworkSource: true, isLive: false, isSeekable: true)
  private var resumeGuard = ResumeGuard()

  private func settings(reconnectAfterMs: Int = 0, stallAfterMs: Int = 0) -> PlayerSettings {
    var settings = PlayerSettings()
    settings.reconnectOnResumeAfterMs = reconnectAfterMs
    settings.reloadOnStallAfterMs = stallAfterMs
    return settings
  }

  private func resumeFromPause(_ settings: PlayerSettings, pausedForMs: Double = longPauseMs) -> ResumePlan {
    resumeGuard.statusChanged(.paused, atMs: 0)
    return resumeGuard.resume(atMs: pausedForMs, positionMs: Self.positionMs, settings: settings, target: networkVideo)
  }

  private func stallCheck(_ plan: ResumePlan) throws -> StallCheck {
    try XCTUnwrap(plan.stallCheck)
  }

  func testAResumeAfterTheLimitReconnectsAtThePosition() {
    let plan = resumeFromPause(settings(reconnectAfterMs: Self.reconnectAfterMs))
    XCTAssertEqual(plan, ResumePlan(reconnectAtMs: Self.positionMs, stallCheck: nil))
  }

  func testAShortPauseOrALocalFileDoesNotReconnect() {
    XCTAssertNil(resumeFromPause(settings(reconnectAfterMs: Self.reconnectAfterMs), pausedForMs: Self.shortPauseMs).reconnectAtMs)
    resumeGuard.statusChanged(.paused, atMs: 0)
    let localFile = ResumeTarget(isNetworkSource: false, isLive: false, isSeekable: true)
    let plan = resumeGuard.resume(atMs: Self.longPauseMs, positionMs: Self.positionMs, settings: settings(reconnectAfterMs: Self.reconnectAfterMs), target: localFile)
    XCTAssertNil(plan.reconnectAtMs)
  }

  func testAResumeWithAStallLimitSchedulesOneCheck() throws {
    let check = try stallCheck(resumeFromPause(settings(stallAfterMs: Self.stallAfterMs)))
    XCTAssertEqual(check.delayMs, Self.stallAfterMs)
    XCTAssertTrue(resumeGuard.isStalled(generation: check.generation, status: .playing, positionMs: Self.positionMs))
    XCTAssertFalse(resumeGuard.isStalled(generation: check.generation, status: .playing, positionMs: Self.positionMs))
  }

  func testABackwardSeekRestartsTheCheckFromTheSeekTarget() throws {
    let beforeSeek = try stallCheck(resumeFromPause(settings(stallAfterMs: Self.stallAfterMs)))

    let afterSeek = try XCTUnwrap(resumeGuard.seek(toMs: Self.seekTargetMs, stallAfterMs: Self.stallAfterMs))

    let positionAtTheCheck = Self.seekTargetMs + Self.progressMs
    XCTAssertFalse(resumeGuard.isStalled(generation: beforeSeek.generation, status: .playing, positionMs: positionAtTheCheck))
    XCTAssertFalse(resumeGuard.isStalled(generation: afterSeek.generation, status: .playing, positionMs: positionAtTheCheck))
  }

  func testASeekWithoutAWaitingCheckSchedulesNothing() {
    XCTAssertNil(resumeGuard.seek(toMs: Self.seekTargetMs, stallAfterMs: Self.stallAfterMs))
  }

  func testASeekAfterTheLimitWasTurnedOffEndsTheCheck() throws {
    let check = try stallCheck(resumeFromPause(settings(stallAfterMs: Self.stallAfterMs)))
    XCTAssertNil(resumeGuard.seek(toMs: Self.seekTargetMs, stallAfterMs: 0))
    XCTAssertFalse(resumeGuard.isStalled(generation: check.generation, status: .buffering, positionMs: Self.seekTargetMs))
  }

  func testAPauseOrAnEndCancelsTheCheck() throws {
    for ending in [PlayerStatus.paused, .stopped, .ended, .error, .opening, .idle] {
      let check = try stallCheck(resumeFromPause(settings(stallAfterMs: Self.stallAfterMs)))
      resumeGuard.statusChanged(ending, atMs: Self.longPauseMs)
      XCTAssertFalse(resumeGuard.isStalled(generation: check.generation, status: .buffering, positionMs: Self.positionMs), "\(ending)")
    }
  }

  func testPlayingAndBufferingKeepTheCheck() throws {
    let check = try stallCheck(resumeFromPause(settings(stallAfterMs: Self.stallAfterMs)))
    resumeGuard.statusChanged(.playing, atMs: 0)
    resumeGuard.statusChanged(.buffering, atMs: 0)
    XCTAssertTrue(resumeGuard.isStalled(generation: check.generation, status: .buffering, positionMs: Self.positionMs))
  }

  func testResetForgetsThePauseAndTheCheck() throws {
    let check = try stallCheck(resumeFromPause(settings(reconnectAfterMs: Self.reconnectAfterMs, stallAfterMs: Self.stallAfterMs)))
    resumeGuard.statusChanged(.paused, atMs: 0)
    resumeGuard.reset()
    XCTAssertFalse(resumeGuard.isStalled(generation: check.generation, status: .buffering, positionMs: Self.positionMs))
    let plan = resumeGuard.resume(atMs: Self.longPauseMs, positionMs: Self.positionMs, settings: settings(reconnectAfterMs: Self.reconnectAfterMs), target: networkVideo)
    XCTAssertNil(plan.reconnectAtMs)
  }
}
