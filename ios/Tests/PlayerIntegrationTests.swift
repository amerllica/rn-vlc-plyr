import MediaPlayer
import XCTest
@testable import RnVlcPlyr

final class PlayerIntegrationTests: XCTestCase {
  private var player: HybridVlcPlayer!
  private var subscriptions: [VlcListenerSubscription] = []

  override func setUp() {
    super.setUp()
    player = HybridVlcPlayer()
  }

  override func tearDown() {
    subscriptions.forEach { $0.remove() }
    subscriptions.removeAll()
    try? player.release()
    player = nil
    super.tearDown()
  }

  private var fixtureUri: String {
    Bundle(for: Self.self).url(forResource: "two-seconds", withExtension: "mp4")?.absoluteString ?? ""
  }

  private var fixtureSource: VlcSource {
    VlcSource(uri: fixtureUri, userAgent: nil, referrer: nil, vlcOptions: [Self.audioOnlyOption], title: nil)
  }

  private static let audioOnlyOption = ":no-video"
  private static let engineTimeout: TimeInterval = 30
  private static let nowPlayingTitle = "Two seconds"

  private static let lateSeekMs = 1500.0

  private var publishedElapsedSeconds: Double? {
    MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPNowPlayingInfoPropertyElapsedPlaybackTime] as? Double
  }

  private var publishedTitle: String? {
    MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPMediaItemPropertyTitle] as? String
  }

  private func drainMainQueue() {
    let drained = expectation(description: "main queue drained")
    DispatchQueue.main.async { drained.fulfill() }
    wait(for: [drained], timeout: 5)
  }

  private func playWithNowPlaying() throws {
    let playing = expectation(description: "playing")
    playing.assertForOverFulfill = false
    subscriptions.append(try player.addOnStatusChangeListener { status in
      if status == .playing { playing.fulfill() }
    })
    player.nowPlaying = VlcNowPlaying(title: Self.nowPlayingTitle, artist: "Artist", album: nil, artworkUri: nil)
    player.source = fixtureSource
    wait(for: [playing], timeout: Self.engineTimeout)
    drainMainQueue()
  }

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
  }

  func testAddSubtitleWithoutSourceThrows() {
    XCTAssertThrowsError(try player.addSubtitle(uri: "file:///a.srt", select: true))
  }

  func testReleaseIsIdempotentAndResetsGettersAndSilencesEvents() throws {
    var statusEvents = 0
    subscriptions.append(try player.addOnStatusChangeListener { _ in statusEvents += 1 })
    player.volume = 10
    player.staysActiveInBackground = true
    player.remoteSkipInterval = 30_000
    player.nowPlaying = VlcNowPlaying(title: Self.nowPlayingTitle, artist: nil, album: nil, artworkUri: nil)
    player.androidNotification = VlcAndroidNotification(color: "#ff0000", smallIcon: nil)
    try player.release()
    try player.release()
    player.source = fixtureSource
    player.volume = 50
    try player.play()
    let settled = expectation(description: "main queue drained")
    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { settled.fulfill() }
    wait(for: [settled], timeout: 2)
    XCTAssertEqual(player.volume, 100)
    XCTAssertNil(player.source)
    XCTAssertEqual(player.status, .idle)
    XCTAssertEqual(statusEvents, 0)
    XCTAssertFalse(player.staysActiveInBackground)
    XCTAssertEqual(player.remoteSkipInterval, 10_000)
    XCTAssertNil(player.nowPlaying)
    XCTAssertNil(player.androidNotification)
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

  func testAndroidNotificationIsStoredAndReturned() {
    player.androidNotification = VlcAndroidNotification(color: "#123456", smallIcon: "ic_play")
    XCTAssertEqual(player.androidNotification?.color, "#123456")
    XCTAssertEqual(player.androidNotification?.smallIcon, "ic_play")
  }

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
