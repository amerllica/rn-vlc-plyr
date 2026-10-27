package com.margelo.nitro.rnvlcplyr.contract

class ResumeReconnect {
  private var pausedAtMs: Long? = null

  fun pausedAt(nowMs: Long) {
    pausedAtMs = nowMs
  }

  fun clear() {
    pausedAtMs = null
  }

  fun shouldReconnect(nowMs: Long, limitMs: Long, isNetworkSource: Boolean, isLive: Boolean, isSeekable: Boolean): Boolean {
    val pausedAt = pausedAtMs ?: return false
    return limitMs > 0L && isNetworkSource && !isLive && isSeekable && nowMs - pausedAt >= limitMs
  }
}
