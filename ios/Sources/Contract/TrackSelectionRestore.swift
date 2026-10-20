import Foundation

struct TrackSelectionRestore: Equatable {
  private struct Pending: Equatable {
    let index: Int
    let expectedCount: Int
  }

  private var audio: Pending?
  private var subtitle: Pending?
  private(set) var selectedExternalSubtitle: Int?

  init() {}

  init(capturing tracks: TracksSnapshot, externalSubtitleCount: Int) {
    audio = Self.pending(tracks.selectedAudioTrack, in: tracks.audioTracks)
    let embeddedCount = max(tracks.subtitleTracks.count - externalSubtitleCount, 0)
    let selectedIndex = tracks.subtitleTracks.firstIndex { $0.id == tracks.selectedSubtitleTrack }
    if let selectedIndex, selectedIndex >= embeddedCount {
      selectedExternalSubtitle = selectedIndex - embeddedCount
    } else {
      subtitle = Self.pending(tracks.selectedSubtitleTrack, in: Array(tracks.subtitleTracks.prefix(embeddedCount)))
    }
  }

  mutating func takeAudioTrack(from tracks: [TrackInfo]) -> Int? {
    Self.take(&audio, from: tracks)
  }

  mutating func takeSubtitleTrack(from tracks: [TrackInfo]) -> Int? {
    Self.take(&subtitle, from: tracks)
  }

  private static func pending(_ selectedId: Int, in tracks: [TrackInfo]) -> Pending? {
    guard !tracks.isEmpty else { return nil }
    let index = tracks.firstIndex { $0.id == selectedId } ?? TrackList.disabledTrackId
    return Pending(index: index, expectedCount: tracks.count)
  }

  private static func take(_ pending: inout Pending?, from tracks: [TrackInfo]) -> Int? {
    guard let current = pending, tracks.count >= current.expectedCount else { return nil }
    pending = nil
    return current.index == TrackList.disabledTrackId ? TrackList.disabledTrackId : tracks[current.index].id
  }
}
