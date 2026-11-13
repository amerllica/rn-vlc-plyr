import MediaPlayer
import UIKit
import XCTest
@testable import RnVlcPlyr

final class MediaSessionIntegrationTests: PlayerTestCase {
  func testLoopRestartPublishesElapsedTimeFromTheStart() throws {
    var lastTime = -1.0
    let looped = expectation(description: "time wrapped back to the start")
    looped.assertForOverFulfill = false
    player.loop = true
    player.timeUpdateInterval = 50
    try playWithNowPlaying()
    subscriptions.append(try player.addOnTimeUpdateListener { event in
      if event.currentTime < lastTime { looped.fulfill() }
      lastTime = event.currentTime
    })
    try player.seek(timeMs: Self.lateSeekMs)
    drainMainQueue()
    XCTAssertGreaterThanOrEqual(try XCTUnwrap(publishedElapsedSeconds), Self.lateSeekMs / 1000 - 0.1)

    wait(for: [looped], timeout: Self.engineTimeout)
    drainMainQueue()

    XCTAssertLessThan(try XCTUnwrap(publishedElapsedSeconds), 1)
  }

  func testNowPlayingIsPublishedWhilePlayingAndClearedWhenUnset() throws {
    try playWithNowPlaying()
    XCTAssertEqual(publishedTitle, Self.nowPlayingTitle)
    XCTAssertEqual(MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPMediaItemPropertyArtist] as? String, "Artist")

    player.nowPlaying = nil
    drainMainQueue()

    XCTAssertNil(publishedTitle)
  }

  func testReleaseClearsNowPlaying() throws {
    try playWithNowPlaying()
    XCTAssertEqual(publishedTitle, Self.nowPlayingTitle)

    try player.release()

    XCTAssertNil(publishedTitle)
  }

  func testASeekOnTheFirstPlayerDoesNotTakeTheSessionBackFromTheSecond() throws {
    player.loop = true
    try playWithNowPlaying()
    let second = HybridVlcPlayer()
    defer { try? second.release() }
    let secondPlaying = expectation(description: "second playing")
    secondPlaying.assertForOverFulfill = false
    subscriptions.append(try second.addOnStatusChangeListener { status in
      if status == .playing { secondPlaying.fulfill() }
    })
    second.loop = true
    second.nowPlaying = VlcNowPlaying(title: Self.secondNowPlayingTitle, artist: nil, album: nil, artworkUri: nil)
    second.source = fixtureSource
    wait(for: [secondPlaying], timeout: Self.engineTimeout)
    drainMainQueue()
    XCTAssertEqual(publishedTitle, Self.secondNowPlayingTitle)

    try player.seek(timeMs: 0)
    drainMainQueue()

    XCTAssertEqual(publishedTitle, Self.secondNowPlayingTitle)
  }

  func testRemotePauseRoutedToOwnerPausesPlayback() throws {
    try playWithNowPlaying()
    let paused = expectation(description: "paused")
    subscriptions.append(try player.addOnStatusChangeListener { status in
      if status == .paused { paused.fulfill() }
    })

    XCTAssertTrue(MediaSessionOwner.shared.route(.pause))

    wait(for: [paused], timeout: Self.engineTimeout)
    XCTAssertEqual(player.status, .paused)
    drainMainQueue()
    XCTAssertEqual(MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPNowPlayingInfoPropertyPlaybackRate] as? Double, 0)
  }

  func testRemoteNextListenerEnablesNextCommandUntilRemoved() throws {
    try playWithNowPlaying()
    let nextCommand = MPRemoteCommandCenter.shared().nextTrackCommand
    XCTAssertFalse(nextCommand.isEnabled)
    XCTAssertEqual(MPRemoteCommandCenter.shared().skipForwardCommand.preferredIntervals, [10])
    var nextEvents = 0

    let subscription = try player.addOnRemoteNextListener { nextEvents += 1 }
    XCTAssertTrue(nextCommand.isEnabled)
    XCTAssertTrue(MediaSessionOwner.shared.route(.next))
    XCTAssertEqual(nextEvents, 1)

    subscription.remove()
    XCTAssertFalse(nextCommand.isEnabled)
  }

  func testRemoteCommandsAreIgnoredWithoutOwner() {
    XCTAssertFalse(MediaSessionOwner.shared.route(.play))
  }
}
