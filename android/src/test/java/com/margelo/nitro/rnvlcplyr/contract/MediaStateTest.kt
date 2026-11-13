package com.margelo.nitro.rnvlcplyr.contract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaStateTest {
  @Test
  fun startsWithoutErrorAndVideoSize() {
    val initial = MediaState()
    assertNull(initial.error)
    assertEquals(MediaState.EMPTY_VIDEO_SIZE, initial.videoSize)
  }

  @Test
  fun isLiveOnlyAfterPlayingWithNoDurationAndNotSeekable() {
    val initial = MediaState()
    assertFalse(initial.isLive)
    assertTrue(initial.copy(hasReachedPlaying = true).isLive)
    assertFalse(initial.copy(hasReachedPlaying = true, isSeekable = true).isLive)
    assertFalse(initial.copy(hasReachedPlaying = true, durationMs = 10).isLive)
  }

  @Test
  fun aSizeIsRenderableWhenBothSidesArePositive() {
    assertFalse(MediaState.isRenderableSize(0, 360))
    assertFalse(MediaState.isRenderableSize(640, 0))
    assertTrue(MediaState.isRenderableSize(1, 1))
  }
}
