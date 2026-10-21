import XCTest
@testable import RnVlcPlyr

final class TrackSelectionRestoreTests: XCTestCase {
  private func slave(_ kind: PlaybackSlave.Kind, _ name: String, select: Bool = false) -> PlaybackSlave {
    PlaybackSlave(kind: kind, url: URL(fileURLWithPath: "/media/\(name)"), select: select)
  }

  func testMapsEmbeddedSelectionByPosition() {
    let before = TracksSnapshot(
      audioTracks: [TrackInfo(id: 1, name: "en"), TrackInfo(id: 2, name: "fr")],
      subtitleTracks: [TrackInfo(id: 3, name: "en"), TrackInfo(id: 4, name: "fr"), TrackInfo(id: 5, name: "file.srt")],
      selectedAudioTrack: 2,
      selectedSubtitleTrack: 4
    )
    var restore = TrackSelectionRestore(capturing: before, slaves: [slave(.subtitle, "file.srt", select: true)])
    XCTAssertEqual(restore.reopenSlaves, [slave(.subtitle, "file.srt")])
    XCTAssertNil(restore.takeSubtitleTrack(from: [TrackInfo(id: 7, name: "en")]))
    XCTAssertEqual(restore.takeSubtitleTrack(from: [TrackInfo(id: 7, name: "en"), TrackInfo(id: 8, name: "fr")]), 8)
    XCTAssertNil(restore.takeSubtitleTrack(from: [TrackInfo(id: 7, name: "en"), TrackInfo(id: 8, name: "fr")]))
    XCTAssertEqual(restore.takeAudioTrack(from: before.audioTracks), 2)
  }

  func testReportsTheSelectedExternalSubtitle() {
    let before = TracksSnapshot(
      subtitleTracks: [TrackInfo(id: 3, name: "en"), TrackInfo(id: 4, name: "a.srt"), TrackInfo(id: 5, name: "b.srt")],
      selectedSubtitleTrack: 5
    )
    var restore = TrackSelectionRestore(capturing: before, slaves: [slave(.subtitle, "a.srt"), slave(.subtitle, "b.srt")])
    XCTAssertEqual(restore.reopenSlaves, [slave(.subtitle, "a.srt"), slave(.subtitle, "b.srt", select: true)])
    XCTAssertNil(restore.takeSubtitleTrack(from: [TrackInfo(id: 7, name: "en"), TrackInfo(id: 8, name: "a.srt")]))
    XCTAssertEqual(
      restore.takeSubtitleTrack(from: [TrackInfo(id: 7, name: "en"), TrackInfo(id: 8, name: "a.srt"), TrackInfo(id: 9, name: "b.srt")]),
      9
    )
  }

  func testKeepsDisabledSubtitlesDisabled() {
    var restore = TrackSelectionRestore(
      capturing: TracksSnapshot(subtitleTracks: [TrackInfo(id: 3, name: "en"), TrackInfo(id: 4, name: "a.srt")]),
      slaves: [slave(.subtitle, "a.srt", select: true)]
    )
    XCTAssertEqual(restore.reopenSlaves, [slave(.subtitle, "a.srt")])
    XCTAssertEqual(restore.takeSubtitleTrack(from: [TrackInfo(id: 3, name: "en")]), TrackList.disabledTrackId)
    XCTAssertNil(restore.takeAudioTrack(from: []))
  }

  func testReportsTheSelectedExternalAudio() {
    let before = TracksSnapshot(
      audioTracks: [TrackInfo(id: 1, name: "en"), TrackInfo(id: 2, name: "dub.m4a")],
      selectedAudioTrack: 2
    )
    var restore = TrackSelectionRestore(capturing: before, slaves: [slave(.audio, "dub.m4a")])
    XCTAssertEqual(restore.reopenSlaves, [slave(.audio, "dub.m4a", select: true)])
    XCTAssertNil(restore.takeAudioTrack(from: [TrackInfo(id: 7, name: "en")]))
    XCTAssertEqual(restore.takeAudioTrack(from: [TrackInfo(id: 7, name: "en"), TrackInfo(id: 8, name: "dub.m4a")]), 8)
  }

  func testMapsEmbeddedAudioByPositionWhenAudioSlavesExist() {
    let before = TracksSnapshot(
      audioTracks: [TrackInfo(id: 1, name: "en"), TrackInfo(id: 2, name: "fr"), TrackInfo(id: 3, name: "dub.m4a")],
      selectedAudioTrack: 2
    )
    var restore = TrackSelectionRestore(capturing: before, slaves: [slave(.audio, "dub.m4a", select: true)])
    XCTAssertEqual(restore.reopenSlaves, [slave(.audio, "dub.m4a")])
    XCTAssertNil(restore.takeAudioTrack(from: [TrackInfo(id: 7, name: "en")]))
    XCTAssertEqual(restore.takeAudioTrack(from: [TrackInfo(id: 7, name: "en"), TrackInfo(id: 8, name: "fr")]), 8)
  }

  func testForgettingTheAudioSelectionKeepsTheSubtitleSelection() {
    let before = TracksSnapshot(
      audioTracks: [TrackInfo(id: 1, name: "en"), TrackInfo(id: 2, name: "fr")],
      subtitleTracks: [TrackInfo(id: 3, name: "en")],
      selectedAudioTrack: 2,
      selectedSubtitleTrack: 3
    )
    var restore = TrackSelectionRestore(capturing: before, slaves: [])
    restore.forgetAudioSelection()
    XCTAssertNil(restore.takeAudioTrack(from: before.audioTracks))
    XCTAssertEqual(restore.takeSubtitleTrack(from: before.subtitleTracks), 3)
  }

  func testReopenSlavesSelectOnlyTheSelectedExternalOfEachKind() {
    let before = TracksSnapshot(
      audioTracks: [TrackInfo(id: 1, name: "en"), TrackInfo(id: 2, name: "a.m4a"), TrackInfo(id: 3, name: "b.m4a")],
      subtitleTracks: [TrackInfo(id: 4, name: "en"), TrackInfo(id: 5, name: "x.srt")],
      selectedAudioTrack: 3,
      selectedSubtitleTrack: 5
    )
    let slaves = [slave(.audio, "a.m4a", select: true), slave(.subtitle, "x.srt"), slave(.audio, "b.m4a")]
    let restore = TrackSelectionRestore(capturing: before, slaves: slaves)
    XCTAssertEqual(
      restore.reopenSlaves,
      [slave(.audio, "a.m4a"), slave(.subtitle, "x.srt", select: true), slave(.audio, "b.m4a", select: true)]
    )
  }
}
