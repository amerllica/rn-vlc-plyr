package com.margelo.nitro.rnvlcplyr.contract

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeUpdateThrottleTest {
  @Test
  fun emitsAtMostOncePerInterval() {
    val throttle = TimeUpdateThrottle(250)
    assertTrue(throttle.shouldEmit(1000))
    assertFalse(throttle.shouldEmit(1100))
    assertFalse(throttle.shouldEmit(1249))
    assertTrue(throttle.shouldEmit(1250))
  }

  @Test
  fun seekEmissionRestartsTheWindow() {
    val throttle = TimeUpdateThrottle(500)
    assertTrue(throttle.shouldEmit(0))
    throttle.markEmitted(400)
    assertFalse(throttle.shouldEmit(600))
    assertTrue(throttle.shouldEmit(900))
  }

  @Test
  fun resetAllowsImmediateEmission() {
    val throttle = TimeUpdateThrottle(5000)
    assertTrue(throttle.shouldEmit(0))
    throttle.reset()
    assertTrue(throttle.shouldEmit(10))
  }

  @Test
  fun aNewIntervalAppliesToTheNextCheck() {
    val throttle = TimeUpdateThrottle(250)
    assertTrue(throttle.shouldEmit(2000))
    throttle.intervalMs = 1000
    assertFalse(throttle.shouldEmit(2900))
    assertTrue(throttle.shouldEmit(3000))
  }
}
