package com.margelo.nitro.rnvlcplyr.contract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSettingsNaNTest {
  @Test
  fun notANumberLeavesEverySettingUnchanged() {
    val settings = PlayerSettings()
    settings.setRate(2.0)
    settings.setTimeUpdateInterval(500.0)
    settings.setSubtitleDelay(300.0)
    settings.setAudioDelay(-200.0)
    settings.volumeControl.setVolume(40.0)

    settings.setRate(Double.NaN)
    settings.setTimeUpdateInterval(Double.NaN)
    settings.setSubtitleDelay(Double.NaN)
    settings.setAudioDelay(Double.NaN)
    val volumeChanged = settings.volumeControl.setVolume(Double.NaN)

    assertEquals(2.0, settings.rate, 0.0)
    assertEquals(500, settings.timeUpdateIntervalMs)
    assertEquals(300, settings.subtitleDelayMs)
    assertEquals(-200, settings.audioDelayMs)
    assertEquals(40, settings.volumeControl.volume)
    assertFalse(volumeChanged)
  }

  @Test
  fun notANumberTurnsTheResumeLimitsOff() {
    val settings = PlayerSettings()
    settings.setReconnectOnResumeAfter(5000.0)
    settings.setReloadOnStallAfter(8000.0)

    settings.setReconnectOnResumeAfter(Double.NaN)
    settings.setReloadOnStallAfter(Double.NaN)

    assertEquals(0L, settings.reconnectOnResumeAfterMs)
    assertEquals(0L, settings.reloadOnStallAfterMs)
  }

  @Test
  fun libvlcOptionsAreTrimmedAndReportOnlyRealChanges() {
    val settings = PlayerSettings()

    assertTrue(settings.setLibvlcOptions(listOf(" --network-caching=1500", "")))
    assertFalse(settings.setLibvlcOptions(listOf("--network-caching=1500 ")))
    assertEquals(listOf("--network-caching=1500"), settings.libvlcOptions)
  }
}
