package com.margelo.nitro.rnvlcplyr

import java.util.concurrent.TimeUnit
import org.junit.Assert.fail

private const val POLL_MS = 50L
const val AWAIT_TIMEOUT_SECONDS = 15L

fun awaitCondition(
  label: String,
  timeoutSeconds: Long = AWAIT_TIMEOUT_SECONDS,
  context: () -> String = { "" },
  condition: () -> Boolean
) {
  val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds)
  while (System.currentTimeMillis() < deadline) {
    if (condition()) return
    Thread.sleep(POLL_MS)
  }
  fail("timed out waiting for $label; ${context()}")
}
