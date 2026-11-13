import MediaPlayer
import UIKit
import XCTest
@testable import RnVlcPlyr

final class ExternalAudioTests: PlayerTestCase {
  func testAddedAudioArrivesThroughTracksChangeAndIsSelected() throws {
    XCTAssertFalse(audioFixtureUri.isEmpty)
    try playVideoWithAudio()
    XCTAssertEqual(player.audioTracks.count, 1)
    try player.addAudio(uri: audioFixtureUri, select: true)
    try waitUntilExternalAudioIsSelected("external audio selected")
  }

  func testAddedAudioStaysInTheTrackListAfterStopAndPlay() throws {
    try playVideoWithAudio()
    try player.addAudio(uri: audioFixtureUri, select: true)
    try waitUntilExternalAudioIsSelected("external audio selected")
    subscriptions.forEach { $0.remove() }
    subscriptions.removeAll()
    try player.stop()
    drainMainQueue()

    try player.play()
    try waitForStatus(.playing)

    try waitForExternalAudioInTheTrackList("external audio listed after stop and play")
  }

  func testAddedAudioStaysInTheTrackListAfterALoop() throws {
    player.loop = true
    try playVideoWithAudio()
    try player.addAudio(uri: audioFixtureUri, select: true)
    try waitUntilExternalAudioIsSelected("external audio selected")
    subscriptions.forEach { $0.remove() }
    subscriptions.removeAll()
    let looped = expectation(description: "time wrapped back to the start")
    looped.assertForOverFulfill = false
    subscriptions.append(try player.addOnTimeUpdateListener { event in
      if event.currentTime < Self.earlyReloadSeekMs { looped.fulfill() }
    })

    try player.seek(timeMs: Self.nearEndSeekMs)
    wait(for: [looped], timeout: Self.engineTimeout)
    try waitForTimeUpdates(Self.settledTimeUpdates)

    try waitForExternalAudioInTheTrackList("external audio listed after the loop")
  }

  func testSubtitleStyleChangeWhilePlayingKeepsExternalAudioSelected() throws {
    try playVideoWithAudio()
    try player.addAudio(uri: audioFixtureUri, select: true)
    try waitUntilExternalAudioIsSelected("external audio selected")
    subscriptions.forEach { $0.remove() }
    subscriptions.removeAll()
    let restored = expectation(description: "external audio selected again after the audio list reloaded")
    restored.assertForOverFulfill = false
    var audioReloaded = false
    subscriptions.append(try player.addOnTracksChangeListener { [unowned self] tracks in
      audioReloaded = audioReloaded || tracks.audioTracks.count < 2
      if audioReloaded && externalAudioIsSelected { restored.fulfill() }
    })
    player.subtitleStyle = Self.yellowSubtitles
    wait(for: [restored], timeout: Self.engineTimeout)
  }

  func testBackgroundKeepsPlayingWhileExternalAudioIsSelected() throws {
    player.staysActiveInBackground = true
    try playVideoWithAudio()
    try player.addAudio(uri: audioFixtureUri, select: true)
    try waitUntilExternalAudioIsSelected("external audio selected")
    postAppState(UIApplication.didEnterBackgroundNotification)
    try assertKeepsPlaying()
    postAppState(UIApplication.willEnterForegroundNotification)
  }

  func testSelectingExternalAudioInBackgroundKeepsPlaying() throws {
    player.staysActiveInBackground = true
    try playVideoWithAudio()
    postAppState(UIApplication.didEnterBackgroundNotification)
    try player.addAudio(uri: audioFixtureUri, select: true)
    try waitUntilExternalAudioIsSelected("external audio selected in the background")
    try assertKeepsPlaying()
    postAppState(UIApplication.willEnterForegroundNotification)
  }
}
