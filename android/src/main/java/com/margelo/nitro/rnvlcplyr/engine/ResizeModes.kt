package com.margelo.nitro.rnvlcplyr.engine

import com.margelo.nitro.rnvlcplyr.VlcResizeMode
import org.videolan.libvlc.MediaPlayer

object ResizeModes {
  val DEFAULT = VlcResizeMode.CONTAIN

  fun scaleTypeFor(mode: VlcResizeMode): MediaPlayer.ScaleType = when (mode) {
    VlcResizeMode.CONTAIN -> MediaPlayer.ScaleType.SURFACE_BEST_FIT
    VlcResizeMode.COVER -> MediaPlayer.ScaleType.SURFACE_FIT_SCREEN
    VlcResizeMode.STRETCH -> MediaPlayer.ScaleType.SURFACE_FILL
    VlcResizeMode.ORIGINAL -> MediaPlayer.ScaleType.SURFACE_ORIGINAL
  }
}
