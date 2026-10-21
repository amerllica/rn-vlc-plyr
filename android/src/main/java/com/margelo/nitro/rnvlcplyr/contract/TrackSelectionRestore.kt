package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcTrack
import com.margelo.nitro.rnvlcplyr.VlcTracksInfo

class TrackSelectionRestore private constructor(
  private var audio: Pending?,
  private var subtitle: Pending?,
  val reopenSlaves: List<PlaybackSlave>
) {
  private data class Pending(val index: Int, val expectedCount: Int)

  private data class Split(val pending: Pending?, val selectedExternal: Int?)

  fun forgetAudioSelection() {
    audio = null
  }

  fun takeAudioTrack(tracks: Array<VlcTrack>): Int? = take(audio, tracks).also { if (it != null) audio = null }

  fun takeSubtitleTrack(tracks: Array<VlcTrack>): Int? = take(subtitle, tracks).also { if (it != null) subtitle = null }

  private fun take(pending: Pending?, tracks: Array<VlcTrack>): Int? {
    if (pending == null || tracks.size < pending.expectedCount) return null
    return if (pending.index == VlcContract.NO_TRACK_ID) VlcContract.NO_TRACK_ID else tracks[pending.index].id.toInt()
  }

  companion object {
    fun none(): TrackSelectionRestore = TrackSelectionRestore(null, null, emptyList())

    fun capture(info: VlcTracksInfo, slaves: List<PlaybackSlave>): TrackSelectionRestore {
      val audio = split(info.selectedAudioTrack.toInt(), info.audioTracks, slaves.count { it.kind == PlaybackSlave.Kind.AUDIO })
      val subtitle = split(info.selectedSubtitleTrack.toInt(), info.subtitleTracks, slaves.count { it.kind == PlaybackSlave.Kind.SUBTITLE })
      val selectedExternal = mapOf(PlaybackSlave.Kind.AUDIO to audio.selectedExternal, PlaybackSlave.Kind.SUBTITLE to subtitle.selectedExternal)
      val seen = mutableMapOf<PlaybackSlave.Kind, Int>()
      val reopenSlaves = slaves.map { slave ->
        val index = seen.getOrDefault(slave.kind, 0)
        seen[slave.kind] = index + 1
        slave.copy(select = selectedExternal[slave.kind] == index)
      }
      return TrackSelectionRestore(audio.pending, subtitle.pending, reopenSlaves)
    }

    private fun split(selectedId: Int, tracks: Array<VlcTrack>, externalCount: Int): Split {
      val embeddedCount = (tracks.size - externalCount).coerceAtLeast(0)
      val selectedIndex = tracks.indexOfFirst { it.id.toInt() == selectedId }
      if (selectedIndex >= embeddedCount) return Split(Pending(selectedIndex, tracks.size), selectedIndex - embeddedCount)
      return Split(pending(selectedId, tracks.copyOfRange(0, embeddedCount)), null)
    }

    private fun pending(selectedId: Int, tracks: Array<VlcTrack>): Pending? {
      if (tracks.isEmpty()) return null
      val index = tracks.indexOfFirst { it.id.toInt() == selectedId }
      return Pending(if (index < 0) VlcContract.NO_TRACK_ID else index, tracks.size)
    }
  }
}
