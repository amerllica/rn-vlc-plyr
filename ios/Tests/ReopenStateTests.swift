import XCTest
@testable import RnVlcPlyr

final class ReopenStateTests: XCTestCase {
  private static let positionMs = 6000
  private static let seekTargetMs = 1000

  private let subtitle = PlaybackSlave(kind: .subtitle, url: URL(fileURLWithPath: "/media/a.srt"), select: true)
  private let tracks = TracksSnapshot(
    audioTracks: [TrackInfo(id: 1, name: "en"), TrackInfo(id: 2, name: "fr")],
    subtitleTracks: [TrackInfo(id: 3, name: "a.srt")],
    selectedAudioTrack: 2,
    selectedSubtitleTrack: 3
  )
  private let reloadedTracks = TracksSnapshot(
    audioTracks: [TrackInfo(id: 7, name: "en"), TrackInfo(id: 8, name: "fr")],
    subtitleTracks: [TrackInfo(id: 9, name: "a.srt")],
    selectedAudioTrack: 7,
    selectedSubtitleTrack: -1
  )

  private var playing: MediaState {
    var media = MediaState()
    media.currentTime = Self.positionMs
    media.isSeekable = true
    media.tracks = tracks
    return media
  }

  private func reloading(isPaused: Bool = false) -> ReopenState {
    var reopen = ReopenState()
    reopen.beginReload(media: playing, hasReachedPlaying: true, isPaused: isPaused, addedSlaves: [subtitle])
    return reopen
  }

  func testAReloadKeepsThePositionThePauseAndTheAddedSlaves() {
    let reopen = reloading(isPaused: true)
    XCTAssertTrue(reopen.isReloadingInPlace)
    XCTAssertEqual(reopen.pendingSeekMs, Self.positionMs)
    XCTAssertTrue(reopen.pausesOnceStarted)
    XCTAssertEqual(reopen.slaves, [subtitle])
  }

  func testASecondReloadBeforePlayingKeepsTheFirstCapture() {
    var reopen = reloading()
    reopen.beginReload(media: MediaState(), hasReachedPlaying: true, isPaused: false, addedSlaves: [])
    XCTAssertEqual(reopen.pendingSeekMs, Self.positionMs)
    XCTAssertEqual(reopen.slaves, [subtitle])
  }

  func testASeekDuringTheReloadReplacesTheRestoredPosition() {
    var reopen = reloading()
    reopen.keepSeek(Self.seekTargetMs)
    XCTAssertEqual(reopen.pendingSeekMs, Self.seekTargetMs)
  }

  func testASeekOutsideAReloadIsNotKept() {
    var reopen = ReopenState()
    reopen.keepSeek(Self.seekTargetMs)
    XCTAssertNil(reopen.pendingSeekMs)
  }

  func testTheRestoredTracksComeBackOnceThePlayerPlays() {
    var reopen = reloading()
    XCTAssertEqual(reopen.tracksArrived(reloadedTracks, isPlaying: false), RestoredTracks(audio: nil, subtitle: nil))
    XCTAssertEqual(reopen.tracksArrived(reloadedTracks, isPlaying: true), RestoredTracks(audio: 8, subtitle: 9))
    XCTAssertEqual(reopen.tracksArrived(reloadedTracks, isPlaying: true), RestoredTracks(audio: nil, subtitle: nil))
  }

  func testForgettingAKindDropsItsRestoreAndItsSlaveSelection() {
    var reopen = reloading()
    reopen.forgetSelection(.subtitle)
    XCTAssertEqual(reopen.tracksArrived(reloadedTracks, isPlaying: true), RestoredTracks(audio: 8, subtitle: nil))
    XCTAssertEqual(reopen.slaves, [PlaybackSlave(kind: .subtitle, url: subtitle.url, select: false)])
  }

  func testAnAddedSlaveWaitsForPlaybackAndIsTakenOnce() {
    var reopen = ReopenState()
    reopen.add(subtitle)
    XCTAssertEqual(reopen.takeSlavesToAttach(), [subtitle])
    XCTAssertEqual(reopen.takeSlavesToAttach(), [])
  }

  func testAUserStopCancelsThePendingStartButKeepsTheSlaves() {
    var reopen = reloading(isPaused: true)
    reopen.cancelPendingStart()
    XCTAssertNil(reopen.pendingSeekMs)
    XCTAssertFalse(reopen.pausesOnceStarted)
    XCTAssertFalse(reopen.isReloadingInPlace)
    XCTAssertEqual(reopen.slaves, [subtitle])
  }

  func testARestartRestoresTheSelectionAndDropsWaitingSlaves() {
    var reopen = ReopenState()
    reopen.add(subtitle)
    reopen.beginRestart(tracks: tracks, addedSlaves: [subtitle])
    XCTAssertEqual(reopen.slaves, [subtitle])
    XCTAssertEqual(reopen.takeSlavesToAttach(), [])
    XCTAssertEqual(reopen.tracksArrived(reloadedTracks, isPlaying: true), RestoredTracks(audio: 8, subtitle: 9))
  }
}
