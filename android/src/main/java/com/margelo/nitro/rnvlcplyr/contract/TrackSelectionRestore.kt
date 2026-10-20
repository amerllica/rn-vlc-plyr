package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcTrack
import com.margelo.nitro.rnvlcplyr.VlcTracksInfo

class TrackSelectionRestore private constructor(
  private var audio: Pending?,
  private var subtitle: Pending?,
  val selectedExternalSubtitle: Int?
) {
  private data class Pending(val index: Int, val expectedCount: Int)

  fun takeAudioTrack(tracks: Array<VlcTrack>): Int? = take(audio, tracks).also { if (it != null) audio = null }

  fun takeSubtitleTrack(tracks: Array<VlcTrack>): Int? = take(subtitle, tracks).also { if (it != null) subtitle = null }

  private fun take(pending: Pending?, tracks: Array<VlcTrack>): Int? {
    if (pending == null || tracks.size < pending.expectedCount) return null
    return if (pending.index == VlcContract.NO_TRACK_ID) VlcContract.NO_TRACK_ID else tracks[pending.index].id.toInt()
  }

  companion object {
    fun none(): TrackSelectionRestore = TrackSelectionRestore(null, null, null)

    fun capture(info: VlcTracksInfo, externalSubtitleCount: Int): TrackSelectionRestore {
      val audio = pending(info.selectedAudioTrack.toInt(), info.audioTracks)
      val embeddedCount = (info.subtitleTracks.size - externalSubtitleCount).coerceAtLeast(0)
      val selectedIndex = info.subtitleTracks.indexOfFirst { it.id == info.selectedSubtitleTrack }
      if (selectedIndex >= embeddedCount) return TrackSelectionRestore(audio, null, selectedIndex - embeddedCount)
      val embedded = info.subtitleTracks.copyOfRange(0, embeddedCount)
      return TrackSelectionRestore(audio, pending(info.selectedSubtitleTrack.toInt(), embedded), null)
    }

    private fun pending(selectedId: Int, tracks: Array<VlcTrack>): Pending? {
      if (tracks.isEmpty()) return null
      val index = tracks.indexOfFirst { it.id.toInt() == selectedId }
      return Pending(if (index < 0) VlcContract.NO_TRACK_ID else index, tracks.size)
    }
  }
}
