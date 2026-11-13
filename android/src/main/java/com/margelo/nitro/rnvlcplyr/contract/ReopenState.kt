package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcTracksInfo

data class RestoredTracks(val audio: Int?, val subtitle: Int?)

class ReopenState(slavesToAddAgain: List<PlaybackSlave> = emptyList()) {
  var pendingSeekMs: Long? = null
  var pausesOnceStarted = false
  var isReloadingInPlace = false
    private set
  var slaves: List<PlaybackSlave> = slavesToAddAgain
    private set

  private var slavesDueAtMs: Long? = null
  private var trackRestore = TrackSelectionRestore.none()
  private var tracksBeforeReopen: VlcTracksInfo? = null

  fun beginReload(media: MediaState, isPaused: Boolean, addedSlaves: List<PlaybackSlave>) {
    if (isReloadingInPlace) return
    isReloadingInPlace = true
    if (pendingSeekMs == null && media.hasReachedPlaying && media.isSeekable && media.currentTimeMs > 0L) {
      pendingSeekMs = media.currentTimeMs
    }
    pausesOnceStarted = pausesOnceStarted || isPaused
    val tracks = tracksBeforeReopen?.takeIf { TrackList.isStillLoading(media.tracks, it) } ?: media.tracks
    tracksBeforeReopen = tracks
    trackRestore = TrackSelectionRestore.capture(tracks, addedSlaves)
    slaves = trackRestore.reopenSlaves
  }

  fun add(slave: PlaybackSlave) {
    slaves = slaves + slave
    if (slave.select) forgetRestore(slave.kind)
  }

  fun keepSeek(targetMs: Long) {
    if (isReloadingInPlace || pendingSeekMs != null) pendingSeekMs = targetMs
  }

  fun forgetSelection(kind: PlaybackSlave.Kind) {
    tracksBeforeReopen = null
    forgetRestore(kind)
    slaves = slaves.map { slave -> if (slave.kind == kind) slave.copy(select = false) else slave }
  }

  fun playbackStarted() {
    isReloadingInPlace = false
  }

  fun cancelPendingStart() {
    pendingSeekMs = null
    pausesOnceStarted = false
    isReloadingInPlace = false
  }

  fun tracksArrived(tracks: VlcTracksInfo): RestoredTracks {
    if (tracksBeforeReopen?.let { TrackList.isStillLoading(tracks, it) } == false) tracksBeforeReopen = null
    return RestoredTracks(
      audio = trackRestore.takeAudioTrack(tracks.audioTracks)?.takeIf { it != tracks.selectedAudioTrack.toInt() },
      subtitle = trackRestore.takeSubtitleTrack(tracks.subtitleTracks)?.takeIf { it != tracks.selectedSubtitleTrack.toInt() }
    )
  }

  fun takeSlavesDue(nowMs: Long, isPlaying: Boolean): List<PlaybackSlave> {
    if (slaves.isEmpty() || pendingSeekMs != null || !isPlaying) return emptyList()
    val dueAtMs = slavesDueAtMs ?: (nowMs + VlcContract.SLAVE_REATTACH_DELAY_MS).also { slavesDueAtMs = it }
    if (nowMs < dueAtMs) return emptyList()
    val due = slaves
    slaves = emptyList()
    slavesDueAtMs = null
    return due
  }

  private fun forgetRestore(kind: PlaybackSlave.Kind) = when (kind) {
    PlaybackSlave.Kind.AUDIO -> trackRestore.forgetAudioSelection()
    PlaybackSlave.Kind.SUBTITLE -> trackRestore.forgetSubtitleSelection()
  }
}
