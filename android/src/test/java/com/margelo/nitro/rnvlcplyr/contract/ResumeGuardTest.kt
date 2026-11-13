package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumeGuardTest {
  private val guard = ResumeGuard()
  private val networkVideo = ResumeTarget(isNetworkSource = true, isLive = false, isSeekable = true)

  private fun settings(reconnectAfterMs: Double = 0.0, stallAfterMs: Double = 0.0) = PlayerSettings().apply {
    setReconnectOnResumeAfter(reconnectAfterMs)
    setReloadOnStallAfter(stallAfterMs)
  }

  private fun resumeFromPause(settings: PlayerSettings, pausedForMs: Long = LONG_PAUSE_MS, positionMs: Long = POSITION_MS): ResumePlan {
    guard.statusChanged(VlcStatus.PAUSED, nowMs = 0L)
    return guard.resume(nowMs = pausedForMs, fromPause = true, positionMs = positionMs, settings = settings, target = networkVideo)
  }

  @Test
  fun aResumeAfterTheLimitReconnectsAtThePosition() {
    val plan = resumeFromPause(settings(reconnectAfterMs = RECONNECT_AFTER_MS))
    assertEquals(POSITION_MS, plan.reconnectAtMs)
    assertNull(plan.stallCheck)
  }

  @Test
  fun aShortPauseOrALocalFileDoesNotReconnect() {
    assertNull(resumeFromPause(settings(reconnectAfterMs = RECONNECT_AFTER_MS), pausedForMs = SHORT_PAUSE_MS).reconnectAtMs)
    guard.statusChanged(VlcStatus.PAUSED, nowMs = 0L)
    val localFile = networkVideo.copy(isNetworkSource = false)
    val plan = guard.resume(LONG_PAUSE_MS, fromPause = true, positionMs = POSITION_MS, settings = settings(reconnectAfterMs = RECONNECT_AFTER_MS), target = localFile)
    assertNull(plan.reconnectAtMs)
  }

  @Test
  fun aPlayThatIsNotAResumeNeitherReconnectsNorChecks() {
    guard.statusChanged(VlcStatus.PAUSED, nowMs = 0L)
    val plan = guard.resume(LONG_PAUSE_MS, fromPause = false, positionMs = POSITION_MS, settings = settings(RECONNECT_AFTER_MS, STALL_AFTER_MS), target = networkVideo)
    assertEquals(ResumePlan(null, null), plan)
  }

  @Test
  fun aResumeWithAStallLimitSchedulesOneCheck() {
    val check = resumeFromPause(settings(stallAfterMs = STALL_AFTER_MS)).stallCheck
    assertEquals(STALL_AFTER_MS.toLong(), check?.delayMs)
    assertTrue(guard.isStalled(checkNotNull(check).generation, VlcStatus.PLAYING, POSITION_MS))
    assertFalse(guard.isStalled(check.generation, VlcStatus.PLAYING, POSITION_MS))
  }

  @Test
  fun aBackwardSeekRestartsTheCheckFromTheSeekTarget() {
    val beforeSeek = checkNotNull(resumeFromPause(settings(stallAfterMs = STALL_AFTER_MS)).stallCheck)

    val afterSeek = guard.seek(SEEK_TARGET_MS, STALL_AFTER_MS.toLong())

    assertNotNull(afterSeek)
    assertFalse(guard.isStalled(beforeSeek.generation, VlcStatus.PLAYING, SEEK_TARGET_MS + PROGRESS_MS))
    assertFalse(guard.isStalled(checkNotNull(afterSeek).generation, VlcStatus.PLAYING, SEEK_TARGET_MS + PROGRESS_MS))
  }

  @Test
  fun aSeekWithoutAWaitingCheckSchedulesNothing() {
    assertNull(guard.seek(SEEK_TARGET_MS, STALL_AFTER_MS.toLong()))
  }

  @Test
  fun aSeekAfterTheLimitWasTurnedOffEndsTheCheck() {
    val check = checkNotNull(resumeFromPause(settings(stallAfterMs = STALL_AFTER_MS)).stallCheck)
    assertNull(guard.seek(SEEK_TARGET_MS, 0L))
    assertFalse(guard.isStalled(check.generation, VlcStatus.BUFFERING, SEEK_TARGET_MS))
  }

  @Test
  fun aPauseOrAnEndCancelsTheCheck() {
    listOf(VlcStatus.PAUSED, VlcStatus.STOPPED, VlcStatus.ENDED, VlcStatus.ERROR, VlcStatus.OPENING, VlcStatus.IDLE).forEach { ending ->
      val check = checkNotNull(resumeFromPause(settings(stallAfterMs = STALL_AFTER_MS)).stallCheck)
      guard.statusChanged(ending, nowMs = LONG_PAUSE_MS)
      assertFalse(guard.isStalled(check.generation, VlcStatus.BUFFERING, POSITION_MS))
    }
  }

  @Test
  fun resetForgetsThePauseAndTheCheck() {
    val check = checkNotNull(resumeFromPause(settings(RECONNECT_AFTER_MS, STALL_AFTER_MS)).stallCheck)
    guard.statusChanged(VlcStatus.PAUSED, nowMs = 0L)
    guard.reset()
    assertFalse(guard.isStalled(check.generation, VlcStatus.BUFFERING, POSITION_MS))
    val plan = guard.resume(LONG_PAUSE_MS, fromPause = true, positionMs = POSITION_MS, settings = settings(reconnectAfterMs = RECONNECT_AFTER_MS), target = networkVideo)
    assertNull(plan.reconnectAtMs)
  }

  private companion object {
    const val RECONNECT_AFTER_MS = 30_000.0
    const val STALL_AFTER_MS = 8_000.0
    const val LONG_PAUSE_MS = 60_000L
    const val SHORT_PAUSE_MS = 5_000L
    const val POSITION_MS = 60_000L
    const val SEEK_TARGET_MS = 10_000L
    const val PROGRESS_MS = 4_000L
  }
}
