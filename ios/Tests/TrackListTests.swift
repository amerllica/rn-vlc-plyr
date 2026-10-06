import XCTest
@testable import RnVlcPlyr

final class TrackListTests: XCTestCase {
  func testDropsDisableEntryAndNamesEmptyTracks() {
    let tracks = TrackList.build(ids: [-1, 1, 2], names: ["Disable", "English", ""])
    XCTAssertEqual(tracks, [TrackInfo(id: 1, name: "English"), TrackInfo(id: 2, name: "Track 2")])
  }

  func testMissingNamesBecomeDefaultNames() {
    XCTAssertEqual(TrackList.build(ids: [3], names: []), [TrackInfo(id: 3, name: "Track 3")])
  }

  func testEmptyEngineListsGiveNoTracks() {
    XCTAssertEqual(TrackList.build(ids: [], names: []), [])
    XCTAssertEqual(TrackList.build(ids: [-1], names: ["Disable"]), [])
  }

  func testSelectableIdsAreKnownIdsOrDisable() {
    let tracks = [TrackInfo(id: 4, name: "a")]
    XCTAssertTrue(TrackList.canSelect(-1, in: tracks))
    XCTAssertTrue(TrackList.canSelect(4, in: tracks))
    XCTAssertFalse(TrackList.canSelect(5, in: tracks))
  }
}

final class ChangeGateTests: XCTestCase {
  func testEmitsOnlyOnChange() {
    var gate = ChangeGate<VolumeState>(initial: VolumeState(volume: 100, muted: false))
    XCTAssertFalse(gate.shouldEmit(VolumeState(volume: 100, muted: false)))
    XCTAssertTrue(gate.shouldEmit(VolumeState(volume: 30, muted: false)))
    XCTAssertFalse(gate.shouldEmit(VolumeState(volume: 30, muted: false)))
    XCTAssertTrue(gate.shouldEmit(VolumeState(volume: 30, muted: true)))
  }

  func testTracksDedupComparesListsAndSelections() {
    var gate = ChangeGate(initial: TracksSnapshot())
    XCTAssertFalse(gate.shouldEmit(TracksSnapshot()))
    let withAudio = TracksSnapshot(audioTracks: [TrackInfo(id: 1, name: "a")], subtitleTracks: [], selectedAudioTrack: 1, selectedSubtitleTrack: -1)
    XCTAssertTrue(gate.shouldEmit(withAudio))
    XCTAssertFalse(gate.shouldEmit(withAudio))
    var reselected = withAudio
    reselected.selectedAudioTrack = -1
    XCTAssertTrue(gate.shouldEmit(reselected))
  }

  func testVideoSizeGateRequiresBothSidesAndChange() {
    var gate = VideoSizeGate()
    XCTAssertFalse(gate.shouldEmit(.zero))
    XCTAssertFalse(gate.shouldEmit(VideoDimensions(width: 640, height: 0)))
    XCTAssertTrue(gate.shouldEmit(VideoDimensions(width: 640, height: 360)))
    XCTAssertFalse(gate.shouldEmit(VideoDimensions(width: 640, height: 360)))
    XCTAssertFalse(gate.shouldEmit(.zero))
    XCTAssertTrue(gate.shouldEmit(VideoDimensions(width: 1280, height: 720)))
    gate.reset()
    XCTAssertTrue(gate.shouldEmit(VideoDimensions(width: 1280, height: 720)))
  }
}

final class TimeUpdateThrottleTests: XCTestCase {
  func testEmitsAtMostOncePerInterval() {
    var throttle = TimeUpdateThrottle()
    XCTAssertTrue(throttle.shouldEmit(atMs: 1000, intervalMs: 250))
    XCTAssertFalse(throttle.shouldEmit(atMs: 1100, intervalMs: 250))
    XCTAssertFalse(throttle.shouldEmit(atMs: 1249, intervalMs: 250))
    XCTAssertTrue(throttle.shouldEmit(atMs: 1250, intervalMs: 250))
  }

  func testSeekEmissionRestartsTheWindow() {
    var throttle = TimeUpdateThrottle()
    XCTAssertTrue(throttle.shouldEmit(atMs: 0, intervalMs: 500))
    throttle.markEmitted(atMs: 400)
    XCTAssertFalse(throttle.shouldEmit(atMs: 600, intervalMs: 500))
    XCTAssertTrue(throttle.shouldEmit(atMs: 900, intervalMs: 500))
  }

  func testResetAllowsImmediateEmission() {
    var throttle = TimeUpdateThrottle()
    XCTAssertTrue(throttle.shouldEmit(atMs: 0, intervalMs: 5000))
    throttle.reset()
    XCTAssertTrue(throttle.shouldEmit(atMs: 10, intervalMs: 5000))
  }
}
