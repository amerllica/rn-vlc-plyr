import MediaPlayer
import UIKit
import XCTest
@testable import RnVlcPlyr

final class PlayerSettingsTests: PlayerTestCase {
  func testDefaults() {
    XCTAssertNil(player.source)
    XCTAssertEqual(player.volume, 100)
    XCTAssertFalse(player.muted)
    XCTAssertEqual(player.rate, 1)
    XCTAssertFalse(player.loop)
    XCTAssertTrue(player.autoPlay)
    XCTAssertEqual(player.timeUpdateInterval, 250)
    XCTAssertEqual(player.status, .idle)
    XCTAssertNil(player.error)
    XCTAssertEqual(player.selectedAudioTrack, -1)
    XCTAssertEqual(player.selectedSubtitleTrack, -1)
    XCTAssertTrue(player.audioTracks.isEmpty)
    XCTAssertEqual(player.videoSize.width, 0)
    XCTAssertFalse(player.staysActiveInBackground)
    XCTAssertNil(player.nowPlaying)
    XCTAssertEqual(player.remoteSkipInterval, 10_000)
    XCTAssertNil(player.androidNotification)
    XCTAssertNil(player.subtitleStyle)
    XCTAssertEqual(player.reconnectOnResumeAfterMs, 0)
    XCTAssertEqual(player.reloadOnStallAfterMs, 0)
    XCTAssertEqual(player.libvlcOptions, [])
  }

  func testSettersClampImmediatelyAndEmitVolumeChangeOnlyOnChange() throws {
    var volumeEvents: [VlcVolumeInfo] = []
    subscriptions.append(try player.addOnVolumeChangeListener { volumeEvents.append($0) })

    player.volume = 250
    player.volume = 100
    player.volume = 30.4
    player.muted = true
    player.muted = true
    player.rate = 10
    player.timeUpdateInterval = 1
    player.subtitleDelay = -99_999
    player.audioDelay = 1234.5

    XCTAssertEqual(player.volume, 30)
    XCTAssertEqual(player.rate, 4)
    XCTAssertEqual(player.timeUpdateInterval, 50)
    XCTAssertEqual(player.subtitleDelay, -60_000)
    XCTAssertEqual(player.audioDelay, 1235)
    XCTAssertEqual(volumeEvents.map(\.volume), [30, 30])
    XCTAssertEqual(volumeEvents.map(\.muted), [false, true])
  }

  func testRecoverySettersNormaliseImmediately() {
    player.reconnectOnResumeAfterMs = 30_000.4
    player.reloadOnStallAfterMs = -1
    player.libvlcOptions = [" --network-caching=1500 ", ""]
    XCTAssertEqual(player.reconnectOnResumeAfterMs, 30_000)
    XCTAssertEqual(player.reloadOnStallAfterMs, 0)
    XCTAssertEqual(player.libvlcOptions, ["--network-caching=1500"])
    player.reloadOnStallAfterMs = .nan
    XCTAssertEqual(player.reloadOnStallAfterMs, 0)
  }

  func testRemoteSkipIntervalDefaultsAndClampsImmediately() {
    XCTAssertEqual(player.remoteSkipInterval, 10_000)
    player.remoteSkipInterval = 100
    XCTAssertEqual(player.remoteSkipInterval, 1000)
    player.remoteSkipInterval = 90_000
    XCTAssertEqual(player.remoteSkipInterval, 60_000)
    player.remoteSkipInterval = 15_000.5
    XCTAssertEqual(player.remoteSkipInterval, 15_001)
    player.remoteSkipInterval = .nan
    XCTAssertEqual(player.remoteSkipInterval, 15_001)
  }

  func testTrackIdsOutsideTheIntegerRangeAreIgnored() throws {
    try player.setAudioTrack(id: .nan)
    try player.setAudioTrack(id: .infinity)
    try player.setSubtitleTrack(id: -.infinity)
    try player.setSubtitleTrack(id: Self.trackIdBeyondIntRange)
    drainMainQueue()

    XCTAssertEqual(player.status, .idle)
  }

  func testAndroidNotificationIsStoredAndReturned() {
    player.androidNotification = VlcAndroidNotification(color: "#123456", smallIcon: "ic_play")
    XCTAssertEqual(player.androidNotification?.color, "#123456")
    XCTAssertEqual(player.androidNotification?.smallIcon, "ic_play")
  }

  func testSubtitleStyleIsNormalisedImmediatelyAndClearedByEmptyStyle() {
    player.subtitleStyle = VlcSubtitleStyle(
      fontFamily: "  Menlo ", fontScale: 5, bold: nil, color: "#abc", backgroundColor: "bad",
      outlineColor: nil, outlineThickness: nil, shadowColor: nil, shadowDistance: 3, marginBottom: nil
    )
    XCTAssertEqual(player.subtitleStyle?.fontFamily, "Menlo")
    XCTAssertEqual(player.subtitleStyle?.fontScale, 25)
    XCTAssertEqual(player.subtitleStyle?.color, "#AABBCC")
    XCTAssertNil(player.subtitleStyle?.backgroundColor)
    XCTAssertEqual(player.subtitleStyle?.shadowDistance, 1)
    player.subtitleStyle = VlcSubtitleStyle(
      fontFamily: nil, fontScale: nil, bold: nil, color: nil, backgroundColor: nil,
      outlineColor: nil, outlineThickness: nil, shadowColor: nil, shadowDistance: nil, marginBottom: nil
    )
    XCTAssertNil(player.subtitleStyle)
  }

  func testAddSubtitleWithoutSourceThrows() {
    XCTAssertThrowsError(try player.addSubtitle(uri: "file:///a.srt", select: true))
  }

  func testAddAudioWithoutSourceThrows() {
    XCTAssertThrowsError(try player.addAudio(uri: "file:///a.m4a", select: true))
  }
}
