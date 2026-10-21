import Foundation

struct TrackSelectionRestore: Equatable {
  private struct Pending: Equatable {
    let index: Int
    let expectedCount: Int
  }

  private struct Split {
    let pending: Pending?
    let selectedExternal: Int?
  }

  private var audio: Pending?
  private var subtitle: Pending?
  private(set) var reopenSlaves: [PlaybackSlave] = []

  init() {}

  init(capturing tracks: TracksSnapshot, slaves: [PlaybackSlave]) {
    let audioSplit = Self.split(tracks.selectedAudioTrack, in: tracks.audioTracks, externalCount: Self.count(.audio, in: slaves))
    let subtitleSplit = Self.split(tracks.selectedSubtitleTrack, in: tracks.subtitleTracks, externalCount: Self.count(.subtitle, in: slaves))
    audio = audioSplit.pending
    subtitle = subtitleSplit.pending
    let selectedExternal: [PlaybackSlave.Kind: Int] = [
      .audio: audioSplit.selectedExternal,
      .subtitle: subtitleSplit.selectedExternal,
    ].compactMapValues { $0 }
    var seen: [PlaybackSlave.Kind: Int] = [:]
    reopenSlaves = slaves.map { slave in
      let index = seen[slave.kind, default: 0]
      seen[slave.kind] = index + 1
      return PlaybackSlave(kind: slave.kind, url: slave.url, select: selectedExternal[slave.kind] == index)
    }
  }

  mutating func forgetAudioSelection() {
    audio = nil
  }

  mutating func takeAudioTrack(from tracks: [TrackInfo]) -> Int? {
    Self.take(&audio, from: tracks)
  }

  mutating func takeSubtitleTrack(from tracks: [TrackInfo]) -> Int? {
    Self.take(&subtitle, from: tracks)
  }

  private static func count(_ kind: PlaybackSlave.Kind, in slaves: [PlaybackSlave]) -> Int {
    slaves.filter { $0.kind == kind }.count
  }

  private static func split(_ selectedId: Int, in tracks: [TrackInfo], externalCount: Int) -> Split {
    let embeddedCount = max(tracks.count - externalCount, 0)
    if let selectedIndex = tracks.firstIndex(where: { $0.id == selectedId }), selectedIndex >= embeddedCount {
      return Split(
        pending: Pending(index: selectedIndex, expectedCount: tracks.count),
        selectedExternal: selectedIndex - embeddedCount
      )
    }
    return Split(pending: pending(selectedId, in: Array(tracks.prefix(embeddedCount))), selectedExternal: nil)
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
