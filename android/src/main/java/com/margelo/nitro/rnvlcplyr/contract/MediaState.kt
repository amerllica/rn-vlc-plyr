package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcError
import com.margelo.nitro.rnvlcplyr.VlcLoadedInfo
import com.margelo.nitro.rnvlcplyr.VlcTimeUpdate
import com.margelo.nitro.rnvlcplyr.VlcTracksInfo
import com.margelo.nitro.rnvlcplyr.VlcVideoSize

data class MediaState(
  val error: VlcError? = null,
  val currentTimeMs: Long = 0L,
  val durationMs: Long = 0L,
  val isSeekable: Boolean = false,
  val hasReachedPlaying: Boolean = false,
  val videoSize: VlcVideoSize = EMPTY_VIDEO_SIZE,
  val tracks: VlcTracksInfo = TrackList.EMPTY_INFO
) {
  val isLive: Boolean
    get() = hasReachedPlaying && durationMs == 0L && !isSeekable

  val timeUpdate: VlcTimeUpdate
    get() = VlcTimeUpdate(currentTimeMs.toDouble(), durationMs.toDouble())

  val loadedInfo: VlcLoadedInfo
    get() = VlcLoadedInfo(
      durationMs.toDouble(),
      isSeekable,
      isLive,
      videoSize,
      tracks.audioTracks,
      tracks.subtitleTracks
    )

  companion object {
    val EMPTY_VIDEO_SIZE = VlcVideoSize(0.0, 0.0)

    fun isRenderableSize(width: Int, height: Int): Boolean = width > 0 && height > 0
  }
}
