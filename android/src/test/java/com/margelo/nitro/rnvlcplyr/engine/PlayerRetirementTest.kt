package com.margelo.nitro.rnvlcplyr.engine

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerRetirementTest {
  @Test
  fun aHungShutdownDoesNotBlockTheNextOne() {
    val hungShutdownMayEnd = CountDownLatch(1)
    val nextShutdownRan = CountDownLatch(1)
    PlayerRetirement.run { hungShutdownMayEnd.await() }

    PlayerRetirement.run { nextShutdownRan.countDown() }

    try {
      assertTrue(nextShutdownRan.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    } finally {
      hungShutdownMayEnd.countDown()
    }
  }

  private companion object {
    const val TIMEOUT_SECONDS = 2L
  }
}
