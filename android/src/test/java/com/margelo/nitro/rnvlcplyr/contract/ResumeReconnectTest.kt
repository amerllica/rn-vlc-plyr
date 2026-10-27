package com.margelo.nitro.rnvlcplyr.contract

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumeReconnectTest {
  private val reconnect = ResumeReconnect()

  private fun shouldReconnect(
    nowMs: Long = PAUSED_AT_MS + LIMIT_MS,
    limitMs: Long = LIMIT_MS,
    isNetworkSource: Boolean = true,
    isLive: Boolean = false,
    isSeekable: Boolean = true
  ) = reconnect.shouldReconnect(nowMs, limitMs, isNetworkSource, isLive, isSeekable)

  @Test
  fun reconnectsANetworkSourceOncePausedForTheLimit() {
    reconnect.pausedAt(PAUSED_AT_MS)

    assertTrue(shouldReconnect())
    assertFalse(shouldReconnect(nowMs = PAUSED_AT_MS + LIMIT_MS - 1))
  }

  @Test
  fun neverReconnectsWithoutARecordedPause() {
    assertFalse(shouldReconnect())

    reconnect.pausedAt(PAUSED_AT_MS)
    reconnect.clear()

    assertFalse(shouldReconnect())
  }

  @Test
  fun zeroLimitTurnsTheReconnectOff() {
    reconnect.pausedAt(PAUSED_AT_MS)

    assertFalse(shouldReconnect(limitMs = 0L, nowMs = PAUSED_AT_MS + 1_000_000L))
  }

  @Test
  fun skipsLocalLiveAndUnseekableSources() {
    reconnect.pausedAt(PAUSED_AT_MS)

    assertFalse(shouldReconnect(isNetworkSource = false))
    assertFalse(shouldReconnect(isLive = true))
    assertFalse(shouldReconnect(isSeekable = false))
  }

  private companion object {
    const val PAUSED_AT_MS = 10_000L
    const val LIMIT_MS = 5_000L
  }
}
