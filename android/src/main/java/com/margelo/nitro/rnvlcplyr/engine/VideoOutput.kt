package com.margelo.nitro.rnvlcplyr.engine

import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

interface VideoOutput {
  val videoLayout: VLCVideoLayout
  val usesTextureView: Boolean
  val scaleType: MediaPlayer.ScaleType

  fun renewVideoLayout()
}
