package com.margelo.nitro.rnvlcplyr

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.margelo.nitro.rnvlcplyr.session.MediaSessionOwner
import com.margelo.nitro.rnvlcplyr.session.PlaybackServiceSetup
import com.margelo.nitro.rnvlcplyr.session.SystemMediaSession
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaSessionTest {
  private lateinit var controller: PlayerController
  private val statuses = CopyOnWriteArrayList<VlcStatus>()

  @Before
  fun setUp() {
    TestMedia.warmLibVLC()
    controller = PlayerController(TestMedia.targetContext)
    controller.events.statusChange.add { statuses.add(it) }
    controller.setLoop(true)
    controller.setAutoPlay(false)
    controller.updateSessionSettings { nowPlaying = NOW_PLAYING }
  }

  @After
  fun tearDown() {
    controller.release()
    awaitCondition("session released") { !onMainSync { SystemMediaSession.isSessionActive } }
  }

  @Test
  fun playingWithNowPlayingPublishesPlayingStateAndMetadata() {
    val session = startPlaying()
    awaitCondition("session playing") { session.playbackState?.state == PlaybackStateCompat.STATE_PLAYING }
    assertEquals(NOW_PLAYING.title, session.metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
    assertEquals(NOW_PLAYING.artist, session.metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
    assertTrue(onMainSync { SystemMediaSession.isSessionActive })
  }

  @Test
  fun transportPauseFromTheSessionPausesThePlayer() {
    val session = startPlaying()
    awaitCondition("session playing") { session.playbackState?.state == PlaybackStateCompat.STATE_PLAYING }
    session.transportControls.pause()
    awaitCondition("player paused") { controller.read { status } == VlcStatus.PAUSED }
    awaitCondition("session paused") { session.playbackState?.state == PlaybackStateCompat.STATE_PAUSED }
  }

  @Test
  fun remoteNextListenerControlsTheSkipToNextAction() {
    val session = startPlaying()
    awaitCondition("session playing") { session.playbackState?.state == PlaybackStateCompat.STATE_PLAYING }
    assertFalse(session.hasAction(PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
    val remoteNextCalls = CopyOnWriteArrayList<Unit>()
    val removeListener = controller.events.remoteNext.add { remoteNextCalls.add(it) }
    awaitCondition("next action added") { session.hasAction(PlaybackStateCompat.ACTION_SKIP_TO_NEXT) }
    session.transportControls.skipToNext()
    awaitCondition("remote next emitted") { remoteNextCalls.size == 1 }
    removeListener()
    awaitCondition("next action removed") { !session.hasAction(PlaybackStateCompat.ACTION_SKIP_TO_NEXT) }
  }

  @Test
  fun clearingNowPlayingDeactivatesTheSession() {
    startPlaying()
    awaitCondition("session active") { onMainSync { SystemMediaSession.isSessionActive } }
    controller.updateSessionSettings { nowPlaying = null }
    awaitCondition("session inactive") { !onMainSync { SystemMediaSession.isSessionActive } }
    assertNull(MediaSessionOwner.current())
    assertEquals(VlcStatus.PLAYING, controller.read { status })
  }

  @Test
  fun missingPlaybackServiceWarnsAndKeepsPlaying() {
    assertFalse(PlaybackServiceSetup.isDeclared(TestMedia.targetContext))
    controller.setStaysActiveInBackground(true)
    val session = startPlaying()
    awaitCondition("session playing") { session.playbackState?.state == PlaybackStateCompat.STATE_PLAYING }
    assertTrue(PlaybackServiceSetup.hasWarned)
    assertTrue(controller.read { settings.staysActiveInBackground })
  }

  @Test
  fun loopRestartPublishesPositionFromTheStart() {
    val session = startPlaying()
    awaitCondition("session playing") { session.playbackState?.state == PlaybackStateCompat.STATE_PLAYING }
    controller.seek(LATE_SEEK_MS)
    awaitCondition("late position published") { session.publishedPosition() >= LATE_SEEK_MS.toLong() }
    awaitCondition("loop restart published") { session.publishedPosition() < LATE_SEEK_MS.toLong() }
    assertEquals(PlaybackStateCompat.STATE_PLAYING, session.playbackState?.state)
  }

  @Test
  fun aSecondPlayerDoesNotTakeAudioFocusFromTheFirst() {
    startPlaying()
    val second = PlayerController(TestMedia.targetContext)
    try {
      second.setLoop(true)
      second.setSource(fixtureSource())
      second.play()
      awaitCondition("second playing") { second.read { status } == VlcStatus.PLAYING }
      Thread.sleep(FOCUS_SETTLE_MS)
      assertEquals(VlcStatus.PLAYING, controller.read { status })
    } finally {
      second.release()
    }
    Thread.sleep(FOCUS_SETTLE_MS)
    assertEquals(VlcStatus.PLAYING, controller.read { status })
  }

  private fun fixtureSource() =
    VlcSource(TestMedia.copyToCache(TestMedia.VIDEO_ASSET).absolutePath, null, null, null, null)

  private fun startPlaying(): MediaControllerCompat {
    controller.setSource(fixtureSource())
    controller.play()
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    awaitCondition("session token") { onMainSync { SystemMediaSession.sessionToken } != null }
    return onMainSync { MediaControllerCompat(TestMedia.targetContext, SystemMediaSession.sessionToken!!) }
  }

  private fun MediaControllerCompat.publishedPosition(): Long = playbackState?.position ?: -1L

  private fun MediaControllerCompat.hasAction(action: Long): Boolean =
    (playbackState?.actions ?: 0L) and action != 0L

  private fun <T> onMainSync(block: () -> T): T {
    val result = AtomicReference<T>()
    InstrumentationRegistry.getInstrumentation().runOnMainSync { result.set(block()) }
    return result.get()
  }

  private fun awaitCondition(label: String, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS)
    while (System.currentTimeMillis() < deadline) {
      if (condition()) return
      Thread.sleep(POLL_MS)
    }
    fail("timed out waiting for $label; statuses=$statuses")
  }

  private companion object {
    const val TIMEOUT_SECONDS = 15L
    const val POLL_MS = 50L
    const val FOCUS_SETTLE_MS = 500L
    const val LATE_SEEK_MS = 1500.0
    val NOW_PLAYING = VlcNowPlaying("Two seconds", "rn-vlc-plyr", "Fixtures", null)
  }
}
