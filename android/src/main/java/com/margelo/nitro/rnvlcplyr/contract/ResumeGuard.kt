package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcStatus

data class StallCheck(val generation: Int, val delayMs: Long)

data class ResumePlan(val reconnectAtMs: Long?, val stallCheck: StallCheck?)

class ResumeGuard {
  private val reconnect = ResumeReconnect()
  private val watchdog = StallWatchdog()

  fun resume(nowMs: Long, fromPause: Boolean, positionMs: Long, settings: PlayerSettings, target: ResumeTarget): ResumePlan {
    val reconnects = fromPause && reconnect.shouldReconnect(nowMs, settings.reconnectOnResumeAfterMs, target)
    reconnect.clear()
    return ResumePlan(
      reconnectAtMs = positionMs.takeIf { reconnects },
      stallCheck = if (fromPause) armStallCheck(positionMs, settings.reloadOnStallAfterMs) else null
    )
  }

  fun seek(positionMs: Long, stallAfterMs: Long): StallCheck? =
    if (watchdog.isArmed) armStallCheck(positionMs, stallAfterMs) else null

  fun statusChanged(status: VlcStatus, nowMs: Long) {
    if (status == VlcStatus.PAUSED) reconnect.pausedAt(nowMs)
    if (status in STALL_CHECK_ENDING_STATUSES) watchdog.disarm()
  }

  fun isStalled(generation: Int, status: VlcStatus, positionMs: Long): Boolean = watchdog.fire(generation, status, positionMs)

  fun reset() {
    reconnect.clear()
    watchdog.disarm()
  }

  private fun armStallCheck(positionMs: Long, delayMs: Long): StallCheck? {
    if (delayMs > 0L) return StallCheck(watchdog.arm(positionMs), delayMs)
    watchdog.disarm()
    return null
  }

  private companion object {
    val STALL_CHECK_ENDING_STATUSES = setOf(
      VlcStatus.IDLE,
      VlcStatus.OPENING,
      VlcStatus.PAUSED,
      VlcStatus.STOPPED,
      VlcStatus.ENDED,
      VlcStatus.ERROR
    )
  }
}
