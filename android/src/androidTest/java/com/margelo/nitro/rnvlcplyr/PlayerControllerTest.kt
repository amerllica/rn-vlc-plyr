package com.margelo.nitro.rnvlcplyr

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
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
class PlayerControllerTest {
  private lateinit var controller: PlayerController
  private val statuses = CopyOnWriteArrayList<VlcStatus>()
  private val loaded = CopyOnWriteArrayList<VlcLoadedInfo>()
  private val times = CopyOnWriteArrayList<VlcTimeUpdate>()
  private val errors = CopyOnWriteArrayList<VlcError>()
  private val tracks = CopyOnWriteArrayList<VlcTracksInfo>()
  private val volumes = CopyOnWriteArrayList<VlcVolumeInfo>()
  private val endings = CopyOnWriteArrayList<Unit>()

  @Before
  fun setUp() {
    TestMedia.warmLibVLC()
    controller = PlayerController(TestMedia.targetContext)
    controller.events.statusChange.add { statuses.add(it) }
    controller.events.loaded.add { loaded.add(it) }
    controller.events.timeUpdate.add { times.add(it) }
    controller.events.error.add { errors.add(it) }
    controller.events.tracksChange.add { tracks.add(it) }
    controller.events.volumeChange.add { volumes.add(it) }
    controller.events.ended.add { endings.add(it) }
  }

  @After
  fun tearDown() {
    controller.release()
  }

  private fun localSource() = VlcSource(TestMedia.copyToCache(TestMedia.VIDEO_ASSET).absolutePath, null, null, null, null)

  @Test
  fun playsLocalFileAndEmitsLoadedOnceAfterPlaying() {
    controller.setSource(localSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    awaitCondition("loaded") { loaded.isNotEmpty() }

    assertEquals(VlcStatus.OPENING, statuses.first())
    assertEquals(statuses.indexOf(VlcStatus.PLAYING) >= 0, true)
    val info = loaded.single()
    assertTrue(info.duration > 0)
    assertTrue(info.isSeekable)
    assertFalse(info.isLive)
    assertEquals(1, info.audioTracks.size)
    assertTrue(controller.read { media.durationMs } > 0)
    awaitCondition("video size") { controller.read { media.videoSize.width } == TestMedia.VIDEO_WIDTH }
    assertEquals(TestMedia.VIDEO_HEIGHT, controller.read { media.videoSize.height }, 0.0)
    assertNoRepeatedStatus()
  }

  @Test
  fun reachesEndedThenEndedEventAndPlayRestarts() {
    controller.setSource(localSource())
    awaitCondition("ended") { endings.isNotEmpty() }
    assertEquals(VlcStatus.ENDED, statuses.last())
    controller.play()
    awaitCondition("replay") { statuses.last() == VlcStatus.PLAYING }
    assertEquals(1, loaded.size)
    assertNoRepeatedStatus()
  }

  @Test
  fun loopRestartsWithoutEndedStatus() {
    controller.setLoop(true)
    controller.setTimeUpdateInterval(FAST_TIME_UPDATE_MS)
    controller.setSource(localSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    awaitCondition("time wrapped back to the start") {
      times.map { it.currentTime }.zipWithNext().any { (previous, next) -> next < previous }
    }
    assertTrue(endings.isEmpty())
    assertFalse(statuses.contains(VlcStatus.ENDED))
    assertEquals(VlcStatus.PLAYING, controller.read { status })
    assertEquals(1, loaded.size)
  }

  @Test
  fun stopEmitsStoppedAndZeroTime() {
    controller.setSource(localSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    controller.stop()
    awaitCondition("stopped") { statuses.last() == VlcStatus.STOPPED }
    awaitCondition("zero time") { times.lastOrNull()?.currentTime == 0.0 }
    assertEquals(0L, controller.read { media.currentTimeMs })
  }

  @Test
  fun seekEmitsTargetTimeImmediately() {
    controller.setLoop(true)
    controller.setSource(localSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    controller.seek(SEEK_TARGET_MS.toDouble())
    awaitCondition("seek time") { times.any { it.currentTime == SEEK_TARGET_MS.toDouble() } }
  }

  @Test
  fun invalidUriFailsWithInvalidSource() {
    controller.setSource(VlcSource("relative/path.mp4", null, null, null, null))
    awaitCondition("error") { errors.isNotEmpty() }
    assertEquals(VlcErrorCode.INVALIDSOURCE, errors.single().code)
    assertEquals(listOf(VlcStatus.ERROR), statuses.toList())
    assertEquals(VlcErrorCode.INVALIDSOURCE, controller.read { media.error }?.code)
  }

  @Test
  fun clearingSourceReturnsToIdleAndResetsMedia() {
    controller.setSource(localSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    controller.setSource(null)
    awaitCondition("idle") { statuses.last() == VlcStatus.IDLE }
    assertEquals(0L, controller.read { media.durationMs })
    assertEquals(0, controller.read { media.tracks.audioTracks.size })
    assertNull(controller.read { source })
  }

  @Test
  fun addedSubtitleArrivesThroughTracksChangeAndIsSelected() {
    controller.setLoop(true)
    controller.setSource(localSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    val subtitle = TestMedia.copyToCache(TestMedia.SUBTITLE_ASSET)
    controller.addSubtitle(subtitle.absolutePath, true)
    awaitCondition("subtitle selected") {
      tracks.any { it.subtitleTracks.isNotEmpty() && it.selectedSubtitleTrack == it.subtitleTracks.first().id }
    }
  }

  @Test
  fun addSubtitleWithoutSourceThrows() {
    try {
      controller.addSubtitle("file:///nothing.srt", true)
      fail("expected addSubtitle to throw")
    } catch (error: VlcPlayerError) {
      assertEquals("addSubtitle requires a source", error.message)
    }
  }

  @Test
  fun settersClampImmediatelyAndVolumeEventsAreDeduplicated() {
    controller.setVolume(150.0)
    controller.setVolume(100.0)
    controller.setMuted(true)
    controller.setRate(10.0)
    controller.setTimeUpdateInterval(1.0)
    controller.setSubtitleDelay(-90000.0)
    assertEquals(100, controller.read { settings.volumeControl.volume })
    assertEquals(4.0, controller.read { settings.rate }, 0.0)
    assertEquals(50, controller.read { settings.timeUpdateIntervalMs })
    assertEquals(-60000, controller.read { settings.subtitleDelayMs })
    assertEquals(listOf(VlcVolumeInfo(100.0, true)), volumes.toList())
  }

  @Test
  fun snapshotWithoutAttachedViewRejects() {
    controller.setSource(localSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    var outcome: Result<String>? = null
    controller.captureSnapshot { outcome = it }
    awaitCondition("snapshot outcome") { outcome != null }
    assertEquals("No video frame available", outcome?.exceptionOrNull()?.message)
  }

  @Test
  fun releaseIsIdempotentSilencesEventsAndResetsGetters() {
    controller.setSource(localSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    controller.setVolume(20.0)
    controller.release()
    controller.release()
    val countAfterRelease = statuses.size
    controller.play()
    controller.setVolume(50.0)
    controller.setSource(localSource())
    Thread.sleep(RELEASE_OBSERVATION_MS)
    assertEquals(countAfterRelease, statuses.size)
    assertEquals(VlcStatus.IDLE, controller.read { status })
    assertEquals(100, controller.read { settings.volumeControl.volume })
    assertNull(controller.read { source })
  }

  private fun assertNoRepeatedStatus() {
    statuses.zipWithNext().forEach { (previous, next) -> assertFalse("repeated $next", previous == next) }
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
    const val FAST_TIME_UPDATE_MS = 50.0
    const val RELEASE_OBSERVATION_MS = 1000L
    const val SEEK_TARGET_MS = 1000L
  }
}
