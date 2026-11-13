package com.margelo.nitro.rnvlcplyr.engine

import com.margelo.nitro.rnvlcplyr.VlcResizeMode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.videolan.libvlc.MediaPlayer

class ResizeModesTest {
  @Test
  fun everyResizeModeHasItsScaleType() {
    assertEquals(MediaPlayer.ScaleType.SURFACE_BEST_FIT, ResizeModes.scaleTypeFor(VlcResizeMode.CONTAIN))
    assertEquals(MediaPlayer.ScaleType.SURFACE_FIT_SCREEN, ResizeModes.scaleTypeFor(VlcResizeMode.COVER))
    assertEquals(MediaPlayer.ScaleType.SURFACE_FILL, ResizeModes.scaleTypeFor(VlcResizeMode.STRETCH))
    assertEquals(MediaPlayer.ScaleType.SURFACE_ORIGINAL, ResizeModes.scaleTypeFor(VlcResizeMode.ORIGINAL))
  }

  @Test
  fun theDefaultIsContain() {
    assertEquals(VlcResizeMode.CONTAIN, ResizeModes.DEFAULT)
  }
}
