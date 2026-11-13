package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StallWatchdogTest {
  private val watchdog = StallWatchdog()

  @Test
  fun firesWhileStillBuffering() {
    val generation = watchdog.arm(POSITION_MS)

    assertTrue(watchdog.fire(generation, VlcStatus.BUFFERING, POSITION_MS + 500L))
  }

  @Test
  fun firesWhenPlayingWithoutProgress() {
    val generation = watchdog.arm(POSITION_MS)

    assertTrue(watchdog.fire(generation, VlcStatus.PLAYING, POSITION_MS))
  }

  @Test
  fun staysQuietOncePlaybackMoves() {
    val generation = watchdog.arm(POSITION_MS)

    assertFalse(watchdog.fire(generation, VlcStatus.PLAYING, POSITION_MS + 1L))
  }

  @Test
  fun firesOncePerResume() {
    val generation = watchdog.arm(POSITION_MS)

    assertTrue(watchdog.fire(generation, VlcStatus.BUFFERING, POSITION_MS))
    assertFalse(watchdog.fire(generation, VlcStatus.BUFFERING, POSITION_MS))
  }

  @Test
  fun disarmedOrOutdatedChecksDoNothing() {
    val first = watchdog.arm(POSITION_MS)
    val second = watchdog.arm(POSITION_MS)

    assertFalse(watchdog.fire(first, VlcStatus.BUFFERING, POSITION_MS))

    watchdog.disarm()

    assertFalse(watchdog.fire(second, VlcStatus.BUFFERING, POSITION_MS))
  }

  @Test
  fun armingAgainAfterASeekMeasuresFromTheSeekTarget() {
    val beforeSeek = watchdog.arm(POSITION_MS)
    assertTrue(watchdog.isArmed)
    val afterSeek = watchdog.arm(SEEK_TARGET_MS)

    assertFalse(watchdog.fire(beforeSeek, VlcStatus.PLAYING, SEEK_TARGET_MS + PROGRESS_MS))
    assertFalse(watchdog.fire(afterSeek, VlcStatus.PLAYING, SEEK_TARGET_MS + PROGRESS_MS))
    assertFalse(watchdog.isArmed)
  }

  @Test
  fun otherStatusesDoNotFire() {
    listOf(VlcStatus.PAUSED, VlcStatus.OPENING, VlcStatus.STOPPED, VlcStatus.ENDED, VlcStatus.ERROR).forEach { status ->
      val generation = watchdog.arm(POSITION_MS)
      assertFalse(watchdog.fire(generation, status, POSITION_MS))
    }
  }

  private companion object {
    const val POSITION_MS = 42_000L
    const val SEEK_TARGET_MS = 10_000L
    const val PROGRESS_MS = 4_000L
  }
}
