package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcVideoSize
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangeFilterTest {
  @Test
  fun passesOnlyChangedValuesAndResets() {
    val filter = ChangeFilter(TrackList.EMPTY_INFO)
    val info = TrackList.info(listOf(EngineTrack(1, "A")), emptyList(), 1, -1)
    assertFalse(filter.offer(TrackList.info(emptyList(), emptyList(), -1, -1)))
    assertTrue(filter.offer(info))
    assertFalse(filter.offer(TrackList.info(listOf(EngineTrack(1, "A")), emptyList(), 1, -1)))
    assertTrue(filter.offer(TrackList.info(listOf(EngineTrack(1, "A")), emptyList(), -1, -1)))
    filter.reset()
    assertTrue(filter.offer(info))
  }

  @Test
  fun dropsEqualVideoSizes() {
    val filter = ChangeFilter(MediaState.EMPTY_VIDEO_SIZE)
    assertTrue(filter.offer(VlcVideoSize(640.0, 360.0)))
    assertFalse(filter.offer(VlcVideoSize(640.0, 360.0)))
    assertTrue(filter.offer(VlcVideoSize(1280.0, 720.0)))
  }
}
