import Foundation

struct RestoredTracks: Equatable {
  let audio: Int?
  let subtitle: Int?
}

struct ReopenState {
  var pendingSeekMs: Int?
  var pausesOnceStarted = false
  private(set) var isReloadingInPlace = false
  private(set) var slaves: [PlaybackSlave] = []
  private var slavesToAttachOncePlaying: [PlaybackSlave] = []
  private var trackRestore = TrackSelectionRestore()
  private var tracksBeforeReopen: TracksSnapshot?

  mutating func beginReload(media: MediaState, hasReachedPlaying: Bool, isPaused: Bool, addedSlaves: [PlaybackSlave]) {
    guard !isReloadingInPlace else { return }
    isReloadingInPlace = true
    if pendingSeekMs == nil && hasReachedPlaying && media.isSeekable && media.currentTime > 0 {
      pendingSeekMs = media.currentTime
    }
    pausesOnceStarted = pausesOnceStarted || isPaused
    let tracks = tracksBeforeReopen.flatMap { TrackList.isStillLoading(media.tracks, comparedTo: $0) ? $0 : nil } ?? media.tracks
    tracksBeforeReopen = tracks
    restoreSelection(of: tracks, addedSlaves: addedSlaves)
  }

  mutating func beginRestart(tracks: TracksSnapshot, addedSlaves: [PlaybackSlave]) {
    restoreSelection(of: tracks, addedSlaves: addedSlaves)
    slavesToAttachOncePlaying.removeAll()
  }

  mutating func add(_ slave: PlaybackSlave) {
    slavesToAttachOncePlaying.append(slave)
    if slave.select {
      forgetRestore(slave.kind)
    }
  }

  mutating func takeSlavesToAttach() -> [PlaybackSlave] {
    defer { slavesToAttachOncePlaying.removeAll() }
    return slavesToAttachOncePlaying
  }

  mutating func keepSeek(_ targetMs: Int) {
    if isReloadingInPlace || pendingSeekMs != nil {
      pendingSeekMs = targetMs
    }
  }

  mutating func forgetSelection(_ kind: PlaybackSlave.Kind) {
    tracksBeforeReopen = nil
    forgetRestore(kind)
    slaves = Self.deselecting(kind, in: slaves)
    slavesToAttachOncePlaying = Self.deselecting(kind, in: slavesToAttachOncePlaying)
  }

  mutating func playbackStarted() {
    isReloadingInPlace = false
  }

  mutating func cancelPendingStart() {
    pendingSeekMs = nil
    pausesOnceStarted = false
    isReloadingInPlace = false
  }

  mutating func tracksArrived(_ tracks: TracksSnapshot, isPlaying: Bool) -> RestoredTracks {
    if let before = tracksBeforeReopen, !TrackList.isStillLoading(tracks, comparedTo: before) {
      tracksBeforeReopen = nil
    }
    guard isPlaying else { return RestoredTracks(audio: nil, subtitle: nil) }
    let audio = trackRestore.takeAudioTrack(from: tracks.audioTracks)
    let subtitle = trackRestore.takeSubtitleTrack(from: tracks.subtitleTracks)
    return RestoredTracks(
      audio: audio == tracks.selectedAudioTrack ? nil : audio,
      subtitle: subtitle == tracks.selectedSubtitleTrack ? nil : subtitle
    )
  }

  private mutating func restoreSelection(of tracks: TracksSnapshot, addedSlaves: [PlaybackSlave]) {
    trackRestore = TrackSelectionRestore(capturing: tracks, slaves: addedSlaves)
    slaves = trackRestore.reopenSlaves
  }

  private mutating func forgetRestore(_ kind: PlaybackSlave.Kind) {
    switch kind {
    case .audio: trackRestore.forgetAudioSelection()
    case .subtitle: trackRestore.forgetSubtitleSelection()
    }
  }

  private static func deselecting(_ kind: PlaybackSlave.Kind, in slaves: [PlaybackSlave]) -> [PlaybackSlave] {
    slaves.map { $0.kind == kind ? PlaybackSlave(kind: $0.kind, url: $0.url, select: false) : $0 }
  }
}
