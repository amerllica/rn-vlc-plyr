package com.margelo.nitro.rnvlcplyr.contract

class TimeUpdateThrottle(var intervalMs: Int = VlcContract.DEFAULT_TIME_UPDATE_INTERVAL_MS) {
  private var lastEmittedAtMs: Long? = null

  fun shouldEmit(nowMs: Long): Boolean {
    val last = lastEmittedAtMs
    if (last != null && nowMs - last < intervalMs) return false
    lastEmittedAtMs = nowMs
    return true
  }

  fun markEmitted(nowMs: Long) {
    lastEmittedAtMs = nowMs
  }

  fun reset() {
    lastEmittedAtMs = null
  }
}
