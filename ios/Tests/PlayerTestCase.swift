import MediaPlayer
import UIKit
import XCTest
@testable import RnVlcPlyr

class PlayerTestCase: XCTestCase {
  var player: HybridVlcPlayer!
  var subscriptions: [VlcListenerSubscription] = []

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

  var fixtureUri: String {
    Bundle(for: Self.self).url(forResource: "two-seconds", withExtension: "mp4")?.absoluteString ?? ""
  }

  var audioFixtureUri: String {
    Bundle(for: Self.self).url(forResource: "grey-ten-seconds", withExtension: "m4a")?.absoluteString ?? ""
  }

  var videoWithAudioSource: VlcSource {
    let uri = Bundle(for: Self.self).url(forResource: "grey-ten-seconds", withExtension: "mp4")?.absoluteString ?? ""
    return VlcSource(uri: uri, userAgent: nil, referrer: nil, vlcOptions: [Self.discardedVideoOutputOption], title: nil)
  }

  var fixtureSource: VlcSource {
    VlcSource(uri: fixtureUri, userAgent: nil, referrer: nil, vlcOptions: [Self.audioOnlyOption], title: nil)
  }

  static let audioOnlyOption = ":no-video"
  static let discardedVideoOutputOption = ":vout=dummy"
  static let engineTimeout: TimeInterval = 30
  static let backgroundPlaySeconds: TimeInterval = 3
  static let nowPlayingTitle = "Two seconds"
  static let secondNowPlayingTitle = "Second player"

  static let trackIdBeyondIntRange = 1e300
  static let lateReloadSeekMs = 6000.0
  static let earlyReloadSeekMs = 1000.0
  static let reloadSeekMidpointMs = 3500.0
  static let settledTimeUpdates = 3
  static let stallCheckMs = 1000.0
  static let stallObservationSeconds: TimeInterval = 2
  static let nearEndSeekMs = 9000.0
  static let rejectedInstanceOption = "--no-such-libvlc-option"
  static let lateSeekMs = 1500.0
  static let restyleSeekMs = 1000.0
  static let restylePositionTolerance = 300.0
  static let yellowSubtitles = VlcSubtitleStyle(
    fontFamily: nil, fontScale: 150, bold: true, color: "#ffff00", backgroundColor: nil,
    outlineColor: nil, outlineThickness: VlcOutlineThickness.thick, shadowColor: nil, shadowDistance: nil, marginBottom: nil
  )

  var publishedElapsedSeconds: Double? {
    MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPNowPlayingInfoPropertyElapsedPlaybackTime] as? Double
  }

  var publishedTitle: String? {
    MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPMediaItemPropertyTitle] as? String
  }

  func drainMainQueue() {
    let drained = expectation(description: "main queue drained")
    DispatchQueue.main.async { drained.fulfill() }
    wait(for: [drained], timeout: 5)
  }

  func waitForStatus(_ wanted: VlcStatus) throws {
    let reached = expectation(description: "status \(wanted.stringValue)")
    reached.assertForOverFulfill = false
    if player.status == wanted { reached.fulfill() }
    subscriptions.append(try player.addOnStatusChangeListener { status in
      if status == wanted { reached.fulfill() }
    })
    wait(for: [reached], timeout: Self.engineTimeout)
    drainMainQueue()
  }

  func waitForTimeUpdates(_ count: Int) throws {
    let settled = expectation(description: "\(count) time updates")
    settled.expectedFulfillmentCount = count
    settled.assertForOverFulfill = false
    subscriptions.append(try player.addOnTimeUpdateListener { _ in settled.fulfill() })
    wait(for: [settled], timeout: Self.engineTimeout)
  }

  func waitForExternalAudioInTheTrackList(_ description: String) throws {
    let listed = expectation(description: description)
    listed.assertForOverFulfill = false
    if player.audioTracks.count == 2 { listed.fulfill() }
    subscriptions.append(try player.addOnTracksChangeListener { tracks in
      if tracks.audioTracks.count == 2 { listed.fulfill() }
    })
    wait(for: [listed], timeout: Self.engineTimeout)
  }

  var externalAudioIsSelected: Bool {
    let tracks = player.audioTracks
    return tracks.count == 2 && player.selectedAudioTrack == tracks.last?.id
  }

  func play(_ source: VlcSource) throws {
    player.source = source
    try waitForStatus(.playing)
  }

  func playVideoWithAudio() throws {
    try play(videoWithAudioSource)
  }

  func waitUntilExternalAudioIsSelected(_ description: String) throws {
    let selected = expectation(description: description)
    selected.assertForOverFulfill = false
    subscriptions.append(try player.addOnTracksChangeListener { [unowned self] _ in
      if externalAudioIsSelected { selected.fulfill() }
    })
    subscriptions.append(try player.addOnTimeUpdateListener { [unowned self] _ in
      if externalAudioIsSelected { selected.fulfill() }
    })
    wait(for: [selected], timeout: Self.engineTimeout)
  }

  func assertKeepsPlaying() throws {
    let ended = expectation(description: "playback ended")
    ended.isInverted = true
    subscriptions.append(try player.addOnStatusChangeListener { status in
      if status == .ended { ended.fulfill() }
    })
    let start = player.currentTime
    wait(for: [ended], timeout: Self.backgroundPlaySeconds)
    XCTAssertEqual(player.status, .playing)
    XCTAssertGreaterThan(player.currentTime, start)
  }

  func postAppState(_ name: Notification.Name) {
    NotificationCenter.default.post(name: name, object: nil)
    drainMainQueue()
  }

  func playWithNowPlaying() throws {
    player.nowPlaying = VlcNowPlaying(title: Self.nowPlayingTitle, artist: "Artist", album: nil, artworkUri: nil)
    try play(fixtureSource)
  }
}
