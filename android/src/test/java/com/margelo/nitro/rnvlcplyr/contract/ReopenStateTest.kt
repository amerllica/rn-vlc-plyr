package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcTrack
import com.margelo.nitro.rnvlcplyr.VlcTracksInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReopenStateTest {
  private val subtitle = PlaybackSlave(PlaybackSlave.Kind.SUBTITLE, "file:///media/a.srt", select = true)
  private val tracks = VlcTracksInfo(
    arrayOf(VlcTrack(1.0, "en"), VlcTrack(2.0, "fr")),
    arrayOf(VlcTrack(3.0, "a.srt")),
    2.0,
    3.0
  )
  private val playing = MediaState(currentTimeMs = POSITION_MS, isSeekable = true, hasReachedPlaying = true, tracks = tracks)

  private fun reloading(isPaused: Boolean = false) = ReopenState().apply { beginReload(playing, isPaused, listOf(subtitle)) }

  @Test
  fun aReloadKeepsThePositionThePauseAndTheAddedSlaves() {
    val reopen = reloading(isPaused = true)
    assertTrue(reopen.isReloadingInPlace)
    assertEquals(POSITION_MS, reopen.pendingSeekMs)
    assertTrue(reopen.pausesOnceStarted)
    assertEquals(listOf(subtitle), reopen.slaves)
  }

  @Test
  fun aSecondReloadBeforePlayingKeepsTheFirstCapture() {
    val reopen = reloading()
    reopen.beginReload(MediaState(), isPaused = false, addedSlaves = emptyList())
    assertEquals(POSITION_MS, reopen.pendingSeekMs)
    assertEquals(listOf(subtitle), reopen.slaves)
  }

  @Test
  fun aSeekDuringTheReloadReplacesTheRestoredPosition() {
    val reopen = reloading()
    reopen.keepSeek(SEEK_TARGET_MS)
    assertEquals(SEEK_TARGET_MS, reopen.pendingSeekMs)
  }

  @Test
  fun aSeekOutsideAReloadIsNotKept() {
    val reopen = ReopenState()
    reopen.keepSeek(SEEK_TARGET_MS)
    assertNull(reopen.pendingSeekMs)
  }

  @Test
  fun theRestoredTracksComeBackOnceTheListsAreComplete() {
    val reopen = reloading()
    val reloaded = VlcTracksInfo(arrayOf(VlcTrack(7.0, "en"), VlcTrack(8.0, "fr")), arrayOf(VlcTrack(9.0, "a.srt")), 7.0, -1.0)
    assertEquals(RestoredTracks(audio = 8, subtitle = 9), reopen.tracksArrived(reloaded))
    assertEquals(RestoredTracks(audio = null, subtitle = null), reopen.tracksArrived(reloaded))
  }

  @Test
  fun forgettingAKindDropsItsRestoreAndItsSlaveSelection() {
    val reopen = reloading()
    reopen.forgetSelection(PlaybackSlave.Kind.SUBTITLE)
    val reloaded = VlcTracksInfo(arrayOf(VlcTrack(7.0, "en"), VlcTrack(8.0, "fr")), arrayOf(VlcTrack(9.0, "a.srt")), 7.0, -1.0)
    assertEquals(RestoredTracks(audio = 8, subtitle = null), reopen.tracksArrived(reloaded))
    assertEquals(listOf(subtitle.copy(select = false)), reopen.slaves)
  }

  @Test
  fun slavesAreDueOneDelayAfterPlaybackSettles() {
    val reopen = reloading()
    assertEquals(emptyList<PlaybackSlave>(), reopen.takeSlavesDue(nowMs = 0L, isPlaying = true))
    reopen.pendingSeekMs = null
    assertEquals(emptyList<PlaybackSlave>(), reopen.takeSlavesDue(nowMs = 0L, isPlaying = false))
    assertEquals(emptyList<PlaybackSlave>(), reopen.takeSlavesDue(nowMs = 0L, isPlaying = true))
    assertEquals(emptyList<PlaybackSlave>(), reopen.takeSlavesDue(nowMs = VlcContract.SLAVE_REATTACH_DELAY_MS - 1, isPlaying = true))
    assertEquals(listOf(subtitle), reopen.takeSlavesDue(nowMs = VlcContract.SLAVE_REATTACH_DELAY_MS, isPlaying = true))
    assertEquals(emptyList<PlaybackSlave>(), reopen.takeSlavesDue(nowMs = 2 * VlcContract.SLAVE_REATTACH_DELAY_MS, isPlaying = true))
  }

  @Test
  fun aUserStopCancelsThePendingStartButKeepsTheSlaves() {
    val reopen = reloading(isPaused = true)
    reopen.cancelPendingStart()
    assertNull(reopen.pendingSeekMs)
    assertFalse(reopen.pausesOnceStarted)
    assertFalse(reopen.isReloadingInPlace)
    assertEquals(listOf(subtitle), reopen.slaves)
  }

  @Test
  fun aRetryStartsWithTheSlavesToAddAgain() {
    assertEquals(listOf(subtitle), ReopenState(listOf(subtitle)).slaves)
  }

  private companion object {
    const val POSITION_MS = 6_000L
    const val SEEK_TARGET_MS = 1_000L
  }
}
