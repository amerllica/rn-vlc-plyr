package com.margelo.nitro.rnvlcplyr.contract

data class ResumeTarget(val isNetworkSource: Boolean, val isLive: Boolean, val isSeekable: Boolean) {
  val canReconnect: Boolean
    get() = isNetworkSource && !isLive && isSeekable
}

class ResumeReconnect {
  private var pausedAtMs: Long? = null

  fun pausedAt(nowMs: Long) {
    pausedAtMs = nowMs
  }

  fun clear() {
    pausedAtMs = null
  }

  fun shouldReconnect(nowMs: Long, limitMs: Long, target: ResumeTarget): Boolean {
    val pausedAt = pausedAtMs ?: return false
    return limitMs > 0L && target.canReconnect && nowMs - pausedAt >= limitMs
  }
}
