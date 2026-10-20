package com.margelo.nitro.rnvlcplyr.engine

import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IVLCVout

class OutputSwitch(
  private val player: MediaPlayer,
  private val isVideoWanted: () -> Boolean
) : IVLCVout.Callback {
  private var surfacesReady = false
  private var voutCount = 0
  private var videoPending = false
  private var subtitlePending = false

  init {
    player.vlcVout.addCallback(this)
  }

  fun begin() {
    if (!player.hasMedia()) return
    videoPending = voutCount > 0
    subtitlePending = true
  }

  fun onVoutCountChanged(count: Int) {
    voutCount = count
    if (count > 0) resendSubtitle() else restoreVideo()
  }

  override fun onSurfacesCreated(vlcVout: IVLCVout) {
    surfacesReady = true
    restoreVideo()
  }

  override fun onSurfacesDestroyed(vlcVout: IVLCVout) {
    surfacesReady = false
  }

  private fun restoreVideo() {
    if (!videoPending || !surfacesReady || voutCount > 0) return
    videoPending = false
    if (isVideoWanted()) player.setVideoTrackEnabled(true)
  }

  private fun resendSubtitle() {
    if (!subtitlePending) return
    subtitlePending = false
    if (player.spuTrack != NO_TRACK && player.isSeekable) player.setTime(player.time)
  }

  private companion object {
    const val NO_TRACK = -1
  }
}
