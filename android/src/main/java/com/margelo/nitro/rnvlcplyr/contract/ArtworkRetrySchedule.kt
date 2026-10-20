package com.margelo.nitro.rnvlcplyr.contract

class ArtworkRetrySchedule {
  var attempts = 0
    private set

  fun nextDelayMs(): Int? {
    val delay = DELAYS_MS.getOrNull(attempts) ?: return null
    attempts += 1
    return delay
  }

  fun reset() {
    attempts = 0
  }

  companion object {
    val DELAYS_MS = listOf(0, 1_000, 3_000, 9_000)
  }
}
