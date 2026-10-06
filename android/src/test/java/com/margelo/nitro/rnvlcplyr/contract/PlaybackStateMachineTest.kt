package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStateMachineTest {
  private var looping = false
  private val machine = PlaybackStateMachine { looping }

  private fun status(value: VlcStatus) = PlaybackEffect.StatusChanged(value)

  private fun playThrough(): List<PlaybackEffect> {
    machine.begin(URI, autoPlay = true)
    machine.onEngineEvent(EngineEvent.OPENING, false)
    return machine.onEngineEvent(EngineEvent.PLAYING, true)
  }

  @Test
  fun startsIdle() {
    assertEquals(VlcStatus.IDLE, machine.status)
    assertEquals(PlayAction.NONE, machine.resolvePlay())
  }

  @Test
  fun autoPlayLoadEmitsOpeningOnceEvenWhenEngineRepeatsIt() {
    assertEquals(listOf(status(VlcStatus.OPENING)), machine.begin(URI, autoPlay = true))
    assertEquals(emptyList<PlaybackEffect>(), machine.onEngineEvent(EngineEvent.OPENING, false))
  }

  @Test
  fun loadWithoutAutoPlayStaysIdleAndPlayResumes() {
    assertEquals(emptyList<PlaybackEffect>(), machine.begin(URI, autoPlay = false))
    assertEquals(VlcStatus.IDLE, machine.status)
    assertEquals(PlayAction.RESUME, machine.resolvePlay())
  }

  @Test
  fun bufferingIsReportedOnlyWhileEngineIsNotPlaying() {
    machine.begin(URI, autoPlay = true)
    assertEquals(listOf(status(VlcStatus.BUFFERING)), machine.onEngineEvent(EngineEvent.BUFFERING, false))
    machine.onEngineEvent(EngineEvent.PLAYING, true)
    assertEquals(emptyList<PlaybackEffect>(), machine.onEngineEvent(EngineEvent.BUFFERING, true))
    assertEquals(VlcStatus.PLAYING, machine.status)
  }

  @Test
  fun bufferingIsIgnoredWhilePaused() {
    playThrough()
    machine.onEngineEvent(EngineEvent.PAUSED, false)
    assertEquals(emptyList<PlaybackEffect>(), machine.onEngineEvent(EngineEvent.BUFFERING, false))
    assertEquals(VlcStatus.PAUSED, machine.status)
  }

  @Test
  fun firstPlayingOfSourceEmitsStatusThenLoadedOnce() {
    assertEquals(listOf(status(VlcStatus.PLAYING), PlaybackEffect.Loaded), playThrough())
    machine.onEngineEvent(EngineEvent.PAUSED, false)
    assertEquals(listOf(status(VlcStatus.PLAYING)), machine.onEngineEvent(EngineEvent.PLAYING, true))
  }

  @Test
  fun loadedFiresAgainForANewSource() {
    playThrough()
    assertEquals(listOf(status(VlcStatus.PLAYING), PlaybackEffect.Loaded), playThrough())
  }

  @Test
  fun statusNeverRepeats() {
    playThrough()
    assertEquals(emptyList<PlaybackEffect>(), machine.onEngineEvent(EngineEvent.PLAYING, true))
    assertEquals(listOf(status(VlcStatus.PAUSED)), machine.onEngineEvent(EngineEvent.PAUSED, false))
    assertEquals(emptyList<PlaybackEffect>(), machine.onEngineEvent(EngineEvent.PAUSED, false))
  }

  @Test
  fun endWithoutLoopEmitsEndedStatusThenEndedEvent() {
    playThrough()
    assertEquals(
      listOf(status(VlcStatus.ENDED), PlaybackEffect.Ended),
      machine.onEngineEvent(EngineEvent.END_REACHED, false)
    )
    assertEquals(emptyList<PlaybackEffect>(), machine.onEngineEvent(EngineEvent.STOPPED, false))
    assertEquals(VlcStatus.ENDED, machine.status)
  }

  @Test
  fun endWithLoopRestartsWithoutLeavingPlaying() {
    looping = true
    playThrough()
    assertEquals(listOf(PlaybackEffect.RestartForLoop), machine.onEngineEvent(EngineEvent.END_REACHED, false))
    assertEquals(emptyList<PlaybackEffect>(), machine.onEngineEvent(EngineEvent.STOPPED, false))
    assertEquals(emptyList<PlaybackEffect>(), machine.onEngineEvent(EngineEvent.OPENING, false))
    assertEquals(emptyList<PlaybackEffect>(), machine.onEngineEvent(EngineEvent.BUFFERING, false))
    assertEquals(emptyList<PlaybackEffect>(), machine.onEngineEvent(EngineEvent.PLAYING, true))
    assertEquals(VlcStatus.PLAYING, machine.status)
    assertTrue(machine.acceptsMediaInfo())
  }

  @Test
  fun engineStoppedEventIsIgnored() {
    playThrough()
    assertEquals(emptyList<PlaybackEffect>(), machine.onEngineEvent(EngineEvent.STOPPED, false))
    assertEquals(VlcStatus.PLAYING, machine.status)
  }

  @Test
  fun engineErrorBeforePlayingOnNetworkSourceIsNetworkError() {
    machine.begin(URI, autoPlay = true)
    assertEquals(
      listOf(status(VlcStatus.ERROR), PlaybackEffect.Failed(VlcContract.NETWORK_ERROR)),
      machine.onEngineEvent(EngineEvent.ERROR, false)
    )
  }

  @Test
  fun engineErrorAfterPlayingIsMediaError() {
    playThrough()
    assertEquals(
      listOf(status(VlcStatus.ERROR), PlaybackEffect.Failed(VlcContract.MEDIA_ERROR)),
      machine.onEngineEvent(EngineEvent.ERROR, false)
    )
  }

  @Test
  fun invalidSourceFailsWithInvalidSourceAndIgnoresEngineEvents() {
    assertEquals(
      listOf(status(VlcStatus.ERROR), PlaybackEffect.Failed(VlcContract.INVALID_SOURCE_ERROR)),
      machine.rejectInvalidSource()
    )
    assertEquals(emptyList<PlaybackEffect>(), machine.onEngineEvent(EngineEvent.PLAYING, true))
    assertEquals(PlayAction.RELOAD, machine.resolvePlay())
  }

  @Test
  fun clearReturnsToIdleAndIgnoresLateEngineEvents() {
    playThrough()
    assertEquals(listOf(status(VlcStatus.IDLE)), machine.clear())
    assertEquals(emptyList<PlaybackEffect>(), machine.onEngineEvent(EngineEvent.PAUSED, false))
    assertEquals(PlayAction.NONE, machine.resolvePlay())
  }

  @Test
  fun playActionDependsOnStatus() {
    playThrough()
    assertEquals(PlayAction.RESUME, machine.resolvePlay())
    machine.onEngineEvent(EngineEvent.END_REACHED, false)
    assertEquals(PlayAction.RESTART, machine.resolvePlay())
    machine.onEngineEvent(EngineEvent.ERROR, false)
    assertEquals(PlayAction.RELOAD, machine.resolvePlay())
  }

  @Test
  fun pauseIsAllowedOnlyWhilePlayingOrBuffering() {
    machine.begin(URI, autoPlay = true)
    assertFalse(machine.canPause())
    machine.onEngineEvent(EngineEvent.BUFFERING, false)
    assertTrue(machine.canPause())
    machine.onEngineEvent(EngineEvent.PLAYING, true)
    assertTrue(machine.canPause())
    machine.onEngineEvent(EngineEvent.PAUSED, false)
    assertFalse(machine.canPause())
  }

  @Test
  fun stopIsNoOpWhenIdleAndOtherwiseStops() {
    assertNull(machine.stop())
    playThrough()
    assertEquals(listOf(status(VlcStatus.STOPPED)), machine.stop())
    assertEquals(PlayAction.RESTART, machine.resolvePlay())
    assertTrue(machine.seekRequiresRestart())
  }

  @Test
  fun mediaInfoIsAcceptedOnlyDuringActivePlayback() {
    assertFalse(machine.acceptsMediaInfo())
    machine.begin(URI, autoPlay = true)
    assertTrue(machine.acceptsMediaInfo())
    machine.onEngineEvent(EngineEvent.PLAYING, true)
    machine.onEngineEvent(EngineEvent.END_REACHED, false)
    assertFalse(machine.acceptsMediaInfo())
  }

  private companion object {
    const val URI = "https://example.com/video.mp4"
  }
}
