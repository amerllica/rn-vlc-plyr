package com.margelo.nitro.rnvlcplyr.contract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtworkRetryScheduleTest {
  private fun ArtworkRetrySchedule.drain(): List<Int> = generateSequence { nextDelayMs() }.toList()

  @Test
  fun firstAttemptHasNoDelay() {
    val schedule = ArtworkRetrySchedule()
    assertEquals(0, schedule.nextDelayMs())
    assertEquals(1, schedule.attempts)
  }

  @Test
  fun retriesWithGrowingDelays() {
    val schedule = ArtworkRetrySchedule()
    schedule.nextDelayMs()
    assertEquals(1_000, schedule.nextDelayMs())
    assertEquals(3_000, schedule.nextDelayMs())
    assertEquals(9_000, schedule.nextDelayMs())
  }

  @Test
  fun givesUpAfterFourAttempts() {
    val schedule = ArtworkRetrySchedule()
    assertEquals(ArtworkRetrySchedule.DELAYS_MS, schedule.drain())
    assertEquals(4, schedule.attempts)
    assertNull(schedule.nextDelayMs())
    assertEquals(4, schedule.attempts)
  }

  @Test
  fun resetStartsOver() {
    val schedule = ArtworkRetrySchedule()
    schedule.drain()
    schedule.reset()
    assertEquals(0, schedule.attempts)
    assertEquals(0, schedule.nextDelayMs())
    assertEquals(1, schedule.attempts)
  }
}
