import MediaPlayer
import UIKit
import XCTest
@testable import RnVlcPlyr

final class PlaybackLifecycleTests: PlayerTestCase {
  func testLocalFileReachesPlayingWithDurationAndFiresLoadedOnce() throws {
    XCTAssertFalse(fixtureUri.isEmpty)
    let playing = expectation(description: "playing")
    let loaded = expectation(description: "loaded")
    var statuses: [VlcStatus] = []
    var loadedDuration = 0.0
    subscriptions.append(try player.addOnStatusChangeListener { status in
      statuses.append(status)
      if status == .playing { playing.fulfill() }
    })
    subscriptions.append(try player.addOnLoadedListener { info in
      loadedDuration = info.duration
      loaded.fulfill()
    })

    player.source = fixtureSource

    wait(for: [playing, loaded], timeout: Self.engineTimeout)
    XCTAssertEqual(statuses.first, .opening)
    XCTAssertGreaterThan(loadedDuration, 0)
    XCTAssertGreaterThan(player.duration, 0)
    XCTAssertTrue(player.isSeekable)
    XCTAssertFalse(player.isLive)
  }

  func testLoadedComesRightAfterTheFirstPlaying() throws {
    var events: [String] = []
    let loaded = expectation(description: "loaded")
    subscriptions.append(try player.addOnStatusChangeListener { events.append("status:\($0.stringValue)") })
    subscriptions.append(try player.addOnTracksChangeListener { _ in events.append("tracks") })
    subscriptions.append(try player.addOnVideoSizeChangeListener { _ in events.append("videoSize") })
    subscriptions.append(try player.addOnLoadedListener { _ in
      events.append("loaded")
      loaded.fulfill()
    })

    player.source = videoWithAudioSource
    wait(for: [loaded], timeout: Self.engineTimeout)

    let playing = try XCTUnwrap(events.firstIndex(of: "status:playing"))
    XCTAssertEqual(events[playing + 1], "loaded", "events=\(events)")
  }

  func testLocalFileEndsWithEndedEvent() throws {
    let ended = expectation(description: "ended")
    var statuses: [VlcStatus] = []
    subscriptions.append(try player.addOnStatusChangeListener { statuses.append($0) })
    subscriptions.append(try player.addOnEndedListener { ended.fulfill() })

    player.source = fixtureSource

    wait(for: [ended], timeout: Self.engineTimeout)
    XCTAssertEqual(statuses.last, .ended, "statuses: \(statuses)")
    XCTAssertEqual(statuses.filter { $0 == .ended }.count, 1)
  }

  func testTimeUpdatesAdvanceWhilePlaying() throws {
    let advanced = expectation(description: "time advanced")
    var times: [Double] = []
    player.timeUpdateInterval = 50
    subscriptions.append(try player.addOnTimeUpdateListener { event in
      times.append(event.currentTime)
      if event.currentTime > 0 && times.count > 1 { advanced.fulfill() }
    })

    player.source = fixtureSource

    wait(for: [advanced], timeout: Self.engineTimeout)
    XCTAssertEqual(times, times.sorted())
  }

  func testLoopRestartsWithoutEndedStatusOrEvent() throws {
    var statuses: [VlcStatus] = []
    var endedEvents = 0
    var lastTime = -1.0
    let looped = expectation(description: "time wrapped back to the start")
    looped.assertForOverFulfill = false
    player.loop = true
    player.timeUpdateInterval = 50
    subscriptions.append(try player.addOnStatusChangeListener { statuses.append($0) })
    subscriptions.append(try player.addOnEndedListener { endedEvents += 1 })
    subscriptions.append(try player.addOnTimeUpdateListener { event in
      if event.currentTime < lastTime { looped.fulfill() }
      lastTime = event.currentTime
    })

    player.source = fixtureSource

    wait(for: [looped], timeout: Self.engineTimeout)
    XCTAssertEqual(endedEvents, 0)
    XCTAssertFalse(statuses.contains(.ended))
    XCTAssertEqual(statuses.last, .playing)
    XCTAssertEqual(player.status, .playing)
  }

  func testBufferingPercentArrivesWhileOpening() throws {
    var percents: [Double] = []
    let buffered = expectation(description: "buffering percent")
    buffered.assertForOverFulfill = false
    subscriptions.append(try player.addOnBufferingListener { event in
      percents.append(event.percent)
      buffered.fulfill()
    })
    player.source = fixtureSource
    wait(for: [buffered], timeout: Self.engineTimeout)
    XCTAssertTrue(percents.allSatisfy { (0...100).contains($0) && $0 == $0.rounded() })
    XCTAssertEqual(percents, percents.reduce(into: [Double]()) { if $0.last != $1 { $0.append($1) } })
  }

  func testInvalidSourceReportsErrorWithoutEngine() throws {
    let failed = expectation(description: "error")
    var received: VlcError?
    subscriptions.append(try player.addOnErrorListener { error in
      received = error
      failed.fulfill()
    })

    player.source = VlcSource(uri: "not a uri", userAgent: nil, referrer: nil, vlcOptions: nil, title: nil)

    wait(for: [failed], timeout: 5)
    XCTAssertEqual(received?.code, .invalidsource)
    XCTAssertEqual(player.status, .error)
    XCTAssertEqual(player.error?.message, "Invalid source uri")
  }

  func testPlayAfterStoppingAnInvalidSourceFailsAgain() throws {
    player.source = VlcSource(uri: "", userAgent: nil, referrer: nil, vlcOptions: nil, title: nil)
    drainMainQueue()
    XCTAssertEqual(player.status, .error)
    try player.stop()
    drainMainQueue()
    XCTAssertEqual(player.status, .stopped)

    try player.play()
    drainMainQueue()

    XCTAssertEqual(player.status, .error)
    XCTAssertEqual(player.error?.code, .invalidsource)
  }

  func testLibvlcOptionsSetBeforeLoadReachesPlaying() throws {
    player.libvlcOptions = ["--network-caching=1500"]
    try playWithNowPlaying()
    XCTAssertEqual(player.status, .playing)
  }

  func testRejectedLibvlcOptionKeepsThePlayerUsable() throws {
    player.libvlcOptions = [Self.rejectedInstanceOption]

    try play(fixtureSource)

    XCTAssertNil(player.error)
  }

  func testBackwardSeekAfterAResumeDoesNotReload() throws {
    player.reloadOnStallAfterMs = Self.stallCheckMs
    try playVideoWithAudio()
    try player.seek(timeMs: Self.lateReloadSeekMs)
    try player.pause()
    try waitForStatus(.paused)
    try player.play()
    try waitForStatus(.playing)
    subscriptions.forEach { $0.remove() }
    subscriptions.removeAll()
    var statuses: [VlcStatus] = []
    let playedPastTheStallCheck = expectation(description: "playback past the stall check")
    playedPastTheStallCheck.assertForOverFulfill = false
    subscriptions.append(try player.addOnStatusChangeListener { statuses.append($0) })
    subscriptions.append(try player.addOnTimeUpdateListener { event in
      if (Self.pastTheStallCheckMs..<Self.lateReloadSeekMs).contains(event.currentTime) { playedPastTheStallCheck.fulfill() }
    })

    try player.seek(timeMs: Self.earlyReloadSeekMs)
    wait(for: [playedPastTheStallCheck], timeout: Self.engineTimeout)

    XCTAssertFalse(statuses.contains(.opening), "reloaded as a stall; statuses=\(statuses.map(\.stringValue))")
  }

  func testStopEmitsStoppedThenATimeUpdateAtZero() throws {
    try playVideoWithAudio()
    var events: [String] = []
    subscriptions.append(try player.addOnStatusChangeListener { events.append("status:\($0.stringValue)") })
    subscriptions.append(try player.addOnTimeUpdateListener { events.append("time:\(Int($0.currentTime))") })

    try player.stop()
    drainMainQueue()

    XCTAssertEqual(Array(events.suffix(2)), ["status:stopped", "time:0"])
    XCTAssertEqual(player.currentTime, 0)
    XCTAssertGreaterThan(player.duration, 0)
  }

  func testSeekByMovesRelativeToTheCurrentTime() throws {
    try playVideoWithAudio()
    try player.seek(timeMs: Self.lateReloadSeekMs)
    try player.pause()
    try waitForStatus(.paused)
    let before = player.currentTime
    var updates: [Double] = []
    subscriptions.append(try player.addOnTimeUpdateListener { updates.append($0.currentTime) })

    try player.seekBy(deltaMs: -Self.earlyReloadSeekMs)
    drainMainQueue()

    XCTAssertEqual(updates.first, before - Self.earlyReloadSeekMs)
  }

  func testReleaseIsIdempotentAndResetsGettersAndSilencesEvents() throws {
    var statusEvents = 0
    subscriptions.append(try player.addOnStatusChangeListener { _ in statusEvents += 1 })
    player.volume = 10
    player.staysActiveInBackground = true
    player.remoteSkipInterval = 30_000
    player.nowPlaying = VlcNowPlaying(title: Self.nowPlayingTitle, artist: nil, album: nil, artworkUri: nil)
    player.androidNotification = VlcAndroidNotification(color: "#ff0000", smallIcon: nil)
    player.subtitleStyle = Self.yellowSubtitles
    try player.release()
    try player.release()
    player.source = fixtureSource
    player.volume = 50
    try player.play()
    drainMainQueue()
    XCTAssertEqual(player.volume, 100)
    XCTAssertNil(player.source)
    XCTAssertEqual(player.status, .idle)
    XCTAssertEqual(statusEvents, 0)
    XCTAssertFalse(player.staysActiveInBackground)
    XCTAssertEqual(player.remoteSkipInterval, 10_000)
    XCTAssertNil(player.nowPlaying)
    XCTAssertNil(player.androidNotification)
    XCTAssertNil(player.subtitleStyle)
  }

  func testReleaseTellsEveryAttachedHost() throws {
    let hosts = [RecordingVideoHost(), RecordingVideoHost()]
    hosts.forEach(player.attach)
    drainMainQueue()

    try player.release()
    drainMainQueue()

    XCTAssertEqual(hosts.map(\.releaseCount), [1, 1])
  }
}


private final class RecordingVideoHost: VideoHost {
  let hostView = UIView()
  let hostResizeMode = ResizeMode.contain
  private(set) var releaseCount = 0

  func playerDidRelease() {
    releaseCount += 1
  }
}
