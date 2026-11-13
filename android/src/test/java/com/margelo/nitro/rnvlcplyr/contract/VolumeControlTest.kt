package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcVolumeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeControlTest {
  @Test
  fun muteIsEmulatedWithoutLosingVolume() {
    val volume = VolumeControl()
    assertEquals(VlcVolumeInfo(100.0, false), volume.info)
    assertTrue(volume.setVolume(30.4))
    assertFalse(volume.setVolume(30.0))
    assertTrue(volume.setMuted(true))
    assertFalse(volume.setMuted(true))
    assertEquals(0, volume.engineVolume)
    assertTrue(volume.setVolume(80.0))
    assertEquals(0, volume.engineVolume)
    assertTrue(volume.setMuted(false))
    assertEquals(80, volume.engineVolume)
    assertEquals(VlcVolumeInfo(80.0, false), volume.info)
  }
}
