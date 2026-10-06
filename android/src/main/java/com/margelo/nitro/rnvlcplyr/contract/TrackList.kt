package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcTrack
import com.margelo.nitro.rnvlcplyr.VlcTracksInfo

data class EngineTrack(val id: Int, val name: String?)

object TrackList {
  val EMPTY_INFO = VlcTracksInfo(
    emptyArray(),
    emptyArray(),
    VlcContract.NO_TRACK_ID.toDouble(),
    VlcContract.NO_TRACK_ID.toDouble()
  )

  fun build(engineTracks: List<EngineTrack>): Array<VlcTrack> =
    engineTracks
      .filter { it.id != VlcContract.NO_TRACK_ID }
      .map { VlcTrack(it.id.toDouble(), displayName(it)) }
      .toTypedArray()

  fun info(
    audio: List<EngineTrack>,
    subtitles: List<EngineTrack>,
    selectedAudio: Int,
    selectedSubtitle: Int
  ): VlcTracksInfo = VlcTracksInfo(build(audio), build(subtitles), selectedAudio.toDouble(), selectedSubtitle.toDouble())

  fun contains(tracks: Array<VlcTrack>, id: Int): Boolean = tracks.any { it.id == id.toDouble() }

  private fun displayName(track: EngineTrack): String =
    track.name?.takeIf { it.isNotEmpty() } ?: "Track ${track.id}"
}
