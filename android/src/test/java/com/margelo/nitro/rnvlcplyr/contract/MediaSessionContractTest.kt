package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSessionContractTest {
  @Test
  fun backgroundWithStaysActiveDisablesVideo() {
    val policy = BackgroundPolicy()
    assertEquals(listOf(BackgroundEffect.DISABLE_VIDEO), policy.enterBackground(staysActive = true, isActive = true))
    assertTrue(policy.inBackground)
    assertTrue(policy.videoDisabled)
  }

  @Test
  fun backgroundWithoutStaysActivePausesWhenActive() {
    val policy = BackgroundPolicy()
    assertEquals(listOf(BackgroundEffect.PAUSE), policy.enterBackground(staysActive = false, isActive = true))
    assertTrue(policy.inBackground)
    assertFalse(policy.videoDisabled)
  }

  @Test
  fun backgroundWithoutStaysActiveDoesNothingWhenInactive() {
    val policy = BackgroundPolicy()
    assertEquals(emptyList<BackgroundEffect>(), policy.enterBackground(staysActive = false, isActive = false))
    assertTrue(policy.inBackground)
  }

  @Test
  fun enteringBackgroundTwiceYieldsNothingTheSecondTime() {
    val policy = BackgroundPolicy()
    policy.enterBackground(staysActive = false, isActive = true)
    assertEquals(emptyList<BackgroundEffect>(), policy.enterBackground(staysActive = false, isActive = true))
    assertEquals(emptyList<BackgroundEffect>(), policy.enterBackground(staysActive = true, isActive = true))
  }

  @Test
  fun foregroundEnablesVideoWhenItWasDisabled() {
    val policy = BackgroundPolicy()
    policy.enterBackground(staysActive = true, isActive = true)
    assertEquals(listOf(BackgroundEffect.ENABLE_VIDEO), policy.enterForeground())
    assertFalse(policy.inBackground)
    assertFalse(policy.videoDisabled)
  }

  @Test
  fun foregroundWithoutDisabledVideoOnlyLeavesBackground() {
    val policy = BackgroundPolicy()
    policy.enterBackground(staysActive = false, isActive = true)
    assertEquals(emptyList<BackgroundEffect>(), policy.enterForeground())
    assertFalse(policy.inBackground)
  }

  @Test
  fun staysActiveChangedOutsideBackgroundYieldsNothing() {
    val policy = BackgroundPolicy()
    assertEquals(emptyList<BackgroundEffect>(), policy.staysActiveChanged(staysActive = false, isActive = true))
    assertEquals(emptyList<BackgroundEffect>(), policy.staysActiveChanged(staysActive = true, isActive = true))
    assertFalse(policy.videoDisabled)
  }

  @Test
  fun staysActiveTurnedOffInBackgroundPausesWhenActiveAndKeepsVideoDisabled() {
    val policy = BackgroundPolicy()
    policy.enterBackground(staysActive = true, isActive = true)
    assertEquals(listOf(BackgroundEffect.PAUSE), policy.staysActiveChanged(staysActive = false, isActive = true))
    assertTrue(policy.videoDisabled)
    assertEquals(listOf(BackgroundEffect.ENABLE_VIDEO), policy.enterForeground())
  }

  @Test
  fun staysActiveTurnedOffInBackgroundWhenInactiveYieldsNothing() {
    val policy = BackgroundPolicy()
    policy.enterBackground(staysActive = true, isActive = false)
    assertEquals(emptyList<BackgroundEffect>(), policy.staysActiveChanged(staysActive = false, isActive = false))
  }

  @Test
  fun staysActiveTurnedOnInBackgroundDisablesVideo() {
    val policy = BackgroundPolicy()
    policy.enterBackground(staysActive = false, isActive = false)
    assertEquals(listOf(BackgroundEffect.DISABLE_VIDEO), policy.staysActiveChanged(staysActive = true, isActive = true))
    assertTrue(policy.videoDisabled)
  }

  @Test
  fun staysActiveTurnedOnInBackgroundWithVideoAlreadyDisabledYieldsNothing() {
    val policy = BackgroundPolicy()
    policy.enterBackground(staysActive = true, isActive = true)
    assertEquals(emptyList<BackgroundEffect>(), policy.staysActiveChanged(staysActive = true, isActive = true))
  }

  @Test
  fun statusIsActiveOnlyForOpeningBufferingAndPlaying() {
    val active = VlcStatus.entries.filter { it.isActive }
    assertEquals(listOf(VlcStatus.OPENING, VlcStatus.BUFFERING, VlcStatus.PLAYING), active)
  }

  @Test
  fun interruptionBeganPausesWhenActiveAndRemembers() {
    val policy = InterruptionPolicy()
    assertEquals(InterruptionEffect.PAUSE, policy.began(isActive = true))
    assertTrue(policy.pausedByInterruption)
  }

  @Test
  fun interruptionBeganWhenInactiveDoesNothing() {
    val policy = InterruptionPolicy()
    assertNull(policy.began(isActive = false))
    assertFalse(policy.pausedByInterruption)
  }

  @Test
  fun interruptionEndedResumesWhenPausedByInterruptionAndShouldResume() {
    val policy = InterruptionPolicy()
    policy.began(isActive = true)
    assertEquals(InterruptionEffect.RESUME, policy.ended(shouldResume = true))
    assertFalse(policy.pausedByInterruption)
  }

  @Test
  fun interruptionEndedWithoutShouldResumeClearsFlag() {
    val policy = InterruptionPolicy()
    policy.began(isActive = true)
    assertNull(policy.ended(shouldResume = false))
    assertFalse(policy.pausedByInterruption)
    assertNull(policy.ended(shouldResume = true))
  }

  @Test
  fun interruptionEndedWithoutPriorPauseDoesNothing() {
    val policy = InterruptionPolicy()
    assertNull(policy.ended(shouldResume = true))
  }

  @Test
  fun permanentLossPausesWhenActiveAndNeverResumes() {
    val policy = InterruptionPolicy()
    policy.began(isActive = true)
    assertEquals(InterruptionEffect.PAUSE, policy.lostPermanently(isActive = true))
    assertFalse(policy.pausedByInterruption)
    assertNull(policy.ended(shouldResume = true))
  }

  @Test
  fun permanentLossWhenInactiveClearsFlagWithoutPausing() {
    val policy = InterruptionPolicy()
    policy.began(isActive = true)
    assertNull(policy.lostPermanently(isActive = false))
    assertFalse(policy.pausedByInterruption)
  }

  @Test
  fun becomingNoisyPausesWhenActiveAndClearsFlag() {
    val policy = InterruptionPolicy()
    policy.began(isActive = true)
    assertEquals(InterruptionEffect.PAUSE, policy.becameNoisy(isActive = true))
    assertFalse(policy.pausedByInterruption)
  }

  @Test
  fun becomingNoisyWhenInactiveDoesNothing() {
    val policy = InterruptionPolicy()
    assertNull(policy.becameNoisy(isActive = false))
    assertFalse(policy.pausedByInterruption)
  }

  @Test
  fun resetClearsPausedByInterruption() {
    val policy = InterruptionPolicy()
    policy.began(isActive = true)
    policy.reset()
    assertFalse(policy.pausedByInterruption)
    assertNull(policy.ended(shouldResume = true))
  }

  @Test
  fun sessionIsActiveOnlyWithNowPlayingAndSource() {
    assertTrue(state(hasNowPlaying = true, hasSource = true).isActive)
    assertFalse(state(hasNowPlaying = true, hasSource = false).isActive)
    assertFalse(state(hasNowPlaying = false, hasSource = true).isActive)
    assertFalse(state(hasNowPlaying = false, hasSource = false).isActive)
  }

  @Test
  fun playbackMapsEveryStatus() {
    val expected = mapOf(
      VlcStatus.IDLE to MediaSessionPlayback.NONE,
      VlcStatus.OPENING to MediaSessionPlayback.BUFFERING,
      VlcStatus.BUFFERING to MediaSessionPlayback.BUFFERING,
      VlcStatus.PLAYING to MediaSessionPlayback.PLAYING,
      VlcStatus.PAUSED to MediaSessionPlayback.PAUSED,
      VlcStatus.STOPPED to MediaSessionPlayback.STOPPED,
      VlcStatus.ENDED to MediaSessionPlayback.STOPPED,
      VlcStatus.ERROR to MediaSessionPlayback.ERROR
    )
    VlcStatus.entries.forEach { status -> assertEquals(expected.getValue(status), state(status = status).playback) }
  }

  @Test
  fun positionAndDurationPassThroughForSeekableMedia() {
    val session = state(currentTimeMs = 4_000L, durationMs = 60_000L)
    assertEquals(4_000L, session.positionMs)
    assertEquals(60_000L, session.durationMs)
  }

  @Test
  fun positionAndDurationAreZeroWhenLive() {
    val session = state(currentTimeMs = 4_000L, durationMs = 60_000L, isSeekable = false, isLive = true)
    assertEquals(0L, session.positionMs)
    assertEquals(0L, session.durationMs)
  }

  @Test
  fun unknownDurationIsZero() {
    assertEquals(0L, state(durationMs = 0L).durationMs)
  }

  @Test
  fun playbackRateIsRateOnlyWhenPlaying() {
    assertEquals(1.5, state(status = VlcStatus.PLAYING, rate = 1.5).playbackRate, 0.0)
    VlcStatus.entries.filter { it != VlcStatus.PLAYING }.forEach { status ->
      assertEquals(0.0, state(status = status, rate = 1.5).playbackRate, 0.0)
    }
  }

  @Test
  fun baseCommandsAreAlwaysAvailable() {
    val commands = state(isSeekable = false).commands
    assertEquals(setOf(RemoteCommand.PLAY, RemoteCommand.PAUSE, RemoteCommand.TOGGLE_PLAY_PAUSE, RemoteCommand.STOP), commands)
  }

  @Test
  fun seekAndSkipCommandsNeedSeekableNonLiveMedia() {
    val seeking = setOf(RemoteCommand.SEEK, RemoteCommand.SKIP_FORWARD, RemoteCommand.SKIP_BACKWARD)
    assertTrue(state(isSeekable = true, isLive = false).commands.containsAll(seeking))
    assertTrue(state(isSeekable = false, isLive = false).commands.intersect(seeking).isEmpty())
    assertTrue(state(isSeekable = true, isLive = true).commands.intersect(seeking).isEmpty())
  }

  @Test
  fun nextAndPreviousFollowListeners() {
    assertFalse(state().commands.contains(RemoteCommand.NEXT))
    assertFalse(state().commands.contains(RemoteCommand.PREVIOUS))
    val withNext = state(hasNextListener = true).commands
    assertTrue(RemoteCommand.NEXT in withNext)
    assertFalse(RemoteCommand.PREVIOUS in withNext)
    val withPrevious = state(hasPreviousListener = true).commands
    assertTrue(RemoteCommand.PREVIOUS in withPrevious)
    assertFalse(RemoteCommand.NEXT in withPrevious)
  }

  @Test
  fun clampSkipIntervalRoundsHalfUpAndClamps() {
    assertEquals(10_000, VlcContract.clampSkipInterval(10_000.0))
    assertEquals(1_501, VlcContract.clampSkipInterval(1_500.5))
    assertEquals(1_500, VlcContract.clampSkipInterval(1_500.49))
    assertEquals(1_000, VlcContract.clampSkipInterval(999.0))
    assertEquals(1_000, VlcContract.clampSkipInterval(-5_000.0))
    assertEquals(60_000, VlcContract.clampSkipInterval(60_001.0))
    assertEquals(60_000, VlcContract.clampSkipInterval(Double.POSITIVE_INFINITY))
  }

  @Test
  fun remoteSkipIntervalDefaultsAndIgnoresNaN() {
    val settings = PlayerSettings()
    assertEquals(10_000, settings.remoteSkipIntervalMs)
    settings.setRemoteSkipInterval(15_000.4)
    settings.setRemoteSkipInterval(Double.NaN)
    assertEquals(15_000, settings.remoteSkipIntervalMs)
  }

  private fun state(
    hasNowPlaying: Boolean = true,
    hasSource: Boolean = true,
    status: VlcStatus = VlcStatus.PLAYING,
    currentTimeMs: Long = 0L,
    durationMs: Long = 10_000L,
    rate: Double = 1.0,
    isSeekable: Boolean = true,
    isLive: Boolean = false,
    hasNextListener: Boolean = false,
    hasPreviousListener: Boolean = false
  ) = MediaSessionState.from(
    hasNowPlaying, hasSource, status, currentTimeMs, durationMs, rate, isSeekable, isLive, hasNextListener, hasPreviousListener
  )
}
