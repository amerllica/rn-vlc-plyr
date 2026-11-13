import MediaPlayer
import UIKit
import XCTest
@testable import RnVlcPlyr

final class InPlaceReloadTests: PlayerTestCase {
  private func assertReopensAndKeepsPosition(after change: () -> Void) throws {
    player.loop = true
    try playWithNowPlaying()
    try player.seek(timeMs: Self.restyleSeekMs)
    drainMainQueue()
    var statuses: [VlcStatus] = []
    var loadedEvents = 0
    let replaying = expectation(description: "playing again after the change")
    replaying.assertForOverFulfill = false
    subscriptions.append(try player.addOnLoadedListener { _ in loadedEvents += 1 })
    subscriptions.append(try player.addOnStatusChangeListener { status in
      statuses.append(status)
      if status == .playing { replaying.fulfill() }
    })
    let restored = expectation(description: "position restored after the change")
    restored.assertForOverFulfill = false

    change()

    wait(for: [replaying], timeout: Self.engineTimeout)
    subscriptions.append(try player.addOnTimeUpdateListener { event in
      if event.currentTime >= Self.restyleSeekMs - Self.restylePositionTolerance { restored.fulfill() }
    })
    wait(for: [restored], timeout: Self.engineTimeout)
    XCTAssertEqual(statuses.first, .opening)
    XCTAssertEqual(loadedEvents, 0)
  }

  func testLibvlcOptionsChangeWhilePlayingReopensAndKeepsPosition() throws {
    try assertReopensAndKeepsPosition { player.libvlcOptions = ["--network-caching=1500"] }
  }

  func testSubtitleStyleSetBeforeLoadReachesPlaying() throws {
    player.subtitleStyle = Self.yellowSubtitles
    try playWithNowPlaying()
    XCTAssertEqual(player.status, .playing)
    XCTAssertEqual(player.subtitleStyle?.fontScale, 150)
  }

  func testSubtitleStyleChangeWhilePlayingReopensAndKeepsPosition() throws {
    try assertReopensAndKeepsPosition { player.subtitleStyle = Self.yellowSubtitles }
  }

  func testSubtitleStyleChangeWhilePausedStaysPaused() throws {
    player.loop = true
    try playWithNowPlaying()
    try player.seek(timeMs: Self.restyleSeekMs)
    try player.pause()
    let paused = expectation(description: "paused")
    paused.assertForOverFulfill = false
    if player.status == .paused { paused.fulfill() }
    subscriptions.append(try player.addOnStatusChangeListener { if $0 == .paused { paused.fulfill() } })
    wait(for: [paused], timeout: Self.engineTimeout)
    subscriptions.removeAll()
    var statuses: [VlcStatus] = []
    let pausedAgain = expectation(description: "paused again after the style change")
    pausedAgain.assertForOverFulfill = false
    subscriptions.append(try player.addOnStatusChangeListener { status in
      statuses.append(status)
      if status == .paused { pausedAgain.fulfill() }
    })
    player.subtitleStyle = Self.yellowSubtitles
    wait(for: [pausedAgain], timeout: Self.engineTimeout)
    drainMainQueue()
    XCTAssertEqual(statuses.first, .opening)
    XCTAssertEqual(player.status, .paused)
    XCTAssertGreaterThanOrEqual(player.currentTime, Self.restyleSeekMs - Self.restylePositionTolerance)
  }

  func testTwoQuickSubtitleStyleChangesStillRestorePosition() throws {
    player.loop = true
    try playWithNowPlaying()
    try player.seek(timeMs: Self.restyleSeekMs)
    drainMainQueue()
    let restored = expectation(description: "position restored after two quick style changes")
    restored.assertForOverFulfill = false
    player.subtitleStyle = Self.yellowSubtitles
    player.subtitleStyle = VlcSubtitleStyle(
      fontFamily: nil, fontScale: 50, bold: nil, color: "#00FFFF", backgroundColor: nil,
      outlineColor: nil, outlineThickness: nil, shadowColor: nil, shadowDistance: nil, marginBottom: nil
    )
    subscriptions.append(try player.addOnTimeUpdateListener { event in
      if event.currentTime >= Self.restyleSeekMs - Self.restylePositionTolerance { restored.fulfill() }
    })
    wait(for: [restored], timeout: Self.engineTimeout)
    XCTAssertEqual(player.subtitleStyle?.fontScale, 50)
  }

  func testDurationStaysKnownWhileAStyleChangeReloads() throws {
    try playVideoWithAudio()
    var durations: [Double] = []
    let replaying = expectation(description: "playing again after the style change")
    replaying.assertForOverFulfill = false
    subscriptions.append(try player.addOnStatusChangeListener { [unowned self] status in
      durations.append(player.duration)
      if status == .playing { replaying.fulfill() }
    })
    subscriptions.append(try player.addOnBufferingListener { [unowned self] _ in durations.append(player.duration) })
    subscriptions.append(try player.addOnTracksChangeListener { [unowned self] _ in durations.append(player.duration) })

    player.subtitleStyle = Self.yellowSubtitles
    wait(for: [replaying], timeout: Self.engineTimeout)

    XCTAssertFalse(durations.isEmpty)
    XCTAssertTrue(durations.allSatisfy { $0 > 0 }, "durations=\(durations)")
  }

  func testSeekDuringAStyleReloadIsKept() throws {
    try playVideoWithAudio()
    try player.seek(timeMs: Self.lateReloadSeekMs)
    drainMainQueue()
    player.subtitleStyle = Self.yellowSubtitles
    drainMainQueue()

    try player.seek(timeMs: Self.earlyReloadSeekMs)
    try waitForStatus(.playing)
    try waitForTimeUpdates(Self.settledTimeUpdates)

    XCTAssertLessThan(player.currentTime, Self.reloadSeekMidpointMs)
  }
}
