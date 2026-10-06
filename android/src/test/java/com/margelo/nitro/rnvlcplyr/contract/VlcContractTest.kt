package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcErrorCode
import com.margelo.nitro.rnvlcplyr.VlcSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VlcContractTest {
  @Test
  fun volumeIsRoundedAndClampedToZeroToHundred() {
    assertEquals(0, VlcContract.clampVolume(-5.0))
    assertEquals(100, VlcContract.clampVolume(250.0))
    assertEquals(43, VlcContract.clampVolume(42.5))
    assertEquals(42, VlcContract.clampVolume(42.4))
    assertEquals(0, VlcContract.clampVolume(Double.NaN))
  }

  @Test
  fun rateIsClampedToQuarterThroughFour() {
    assertEquals(0.25, VlcContract.clampRate(0.1), 0.0)
    assertEquals(4.0, VlcContract.clampRate(9.0), 0.0)
    assertEquals(1.5, VlcContract.clampRate(1.5), 0.0)
    assertEquals(VlcContract.DEFAULT_RATE, VlcContract.clampRate(Double.NaN), 0.0)
  }

  @Test
  fun timeUpdateIntervalIsRoundedAndClamped() {
    assertEquals(50, VlcContract.clampTimeUpdateInterval(10.0))
    assertEquals(5000, VlcContract.clampTimeUpdateInterval(9000.0))
    assertEquals(251, VlcContract.clampTimeUpdateInterval(250.6))
  }

  @Test
  fun delaysAreRoundedClampedAndConvertedToMicroseconds() {
    assertEquals(-60000, VlcContract.clampDelay(-70000.0))
    assertEquals(60000, VlcContract.clampDelay(70000.0))
    assertEquals(501, VlcContract.clampDelay(500.5))
    assertEquals(500_000L, VlcContract.delayToMicros(500))
    assertEquals(-1_000L, VlcContract.delayToMicros(-1))
  }

  @Test
  fun roundingFollowsJavaScriptMathRound() {
    assertEquals(0L, VlcContract.roundHalfUp(-0.5))
    assertEquals(-1L, VlcContract.roundHalfUp(-0.6))
    assertEquals(3L, VlcContract.roundHalfUp(2.5))
  }

  @Test
  fun seekIsRoundedAndClampedToDurationWhenKnown() {
    assertEquals(0L, VlcContract.clampSeek(-100.0, 10_000))
    assertEquals(10_000L, VlcContract.clampSeek(12_000.0, 10_000))
    assertEquals(1235L, VlcContract.clampSeek(1234.5, 10_000))
    assertEquals(99_999L, VlcContract.clampSeek(99_999.0, 0))
  }

  @Test
  fun durationIsZeroWhenEngineReportsNonPositiveLength() {
    assertEquals(0L, VlcContract.durationFromEngine(-1))
    assertEquals(0L, VlcContract.durationFromEngine(0))
    assertEquals(1500L, VlcContract.durationFromEngine(1500))
  }

  @Test
  fun uriWithSchemeIsKeptAndAbsolutePathBecomesFileUri() {
    assertEquals("https://a.b/c.mp4", VlcContract.resolveMediaUri("https://a.b/c.mp4"))
    assertEquals("rtsp://cam/stream", VlcContract.resolveMediaUri("rtsp://cam/stream"))
    assertEquals("file:///sdcard/a.mp4", VlcContract.resolveMediaUri("/sdcard/a.mp4"))
    assertEquals("content://media/1", VlcContract.resolveMediaUri("content://media/1"))
  }

  @Test
  fun emptyOrSchemelessRelativeUriIsInvalid() {
    assertNull(VlcContract.resolveMediaUri(""))
    assertNull(VlcContract.resolveMediaUri("video.mp4"))
    assertNull(VlcContract.resolveMediaUri("  "))
    assertNull(VlcContract.resolveMediaUri("1http://x"))
  }

  @Test
  fun mediaOptionsAreUserAgentThenReferrerThenVerbatimOptions() {
    val source = VlcSource("https://x", "UA/1", "https://ref", arrayOf(":network-caching=300", "--no-audio"), null)
    assertEquals(
      listOf(":http-user-agent=UA/1", ":http-referrer=https://ref", ":network-caching=300", "--no-audio"),
      VlcContract.mediaOptions(source)
    )
    assertEquals(emptyList<String>(), VlcContract.mediaOptions(VlcSource("https://x", null, null, null, null)))
  }

  @Test
  fun engineErrorBeforeFirstPlayOnNetworkSchemeIsNetworkError() {
    listOf("http", "https", "rtsp", "rtmp", "rtp", "udp", "mms", "ftp", "smb", "HTTPS").forEach { scheme ->
      assertEquals(VlcErrorCode.NETWORK, VlcContract.classifyEngineError("$scheme://host/x", false).code)
    }
  }

  @Test
  fun otherEngineErrorsAreMediaErrors() {
    assertEquals(VlcContract.MEDIA_ERROR, VlcContract.classifyEngineError("https://host/x", true))
    assertEquals(VlcContract.MEDIA_ERROR, VlcContract.classifyEngineError("file:///a.mp4", false))
    assertEquals(VlcContract.MEDIA_ERROR, VlcContract.classifyEngineError(null, false))
  }

  @Test
  fun errorMessagesAreTheFixedContractStrings() {
    assertEquals("Invalid source uri", VlcContract.INVALID_SOURCE_ERROR.message)
    assertEquals(VlcErrorCode.INVALIDSOURCE, VlcContract.INVALID_SOURCE_ERROR.code)
    assertEquals("Could not open the media over the network", VlcContract.NETWORK_ERROR.message)
    assertEquals("The media could not be played", VlcContract.MEDIA_ERROR.message)
  }
}
