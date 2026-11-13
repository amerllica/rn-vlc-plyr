package com.margelo.nitro.rnvlcplyr

import android.os.Handler
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.margelo.nitro.rnvlcplyr.contract.VlcContract
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
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
  private val bufferings = CopyOnWriteArrayList<VlcBufferingInfo>()

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
    controller.events.buffering.add { bufferings.add(it) }
  }

  @After
  fun tearDown() {
    controller.release()
  }

  private fun localSource() = TestMedia.assetSource(TestMedia.VIDEO_ASSET)

  private fun tenSecondSource() = TestMedia.assetSource(TestMedia.PLAIN_VIDEO_ASSET)

  private fun assertKeepsPlaying() {
    val start = controller.read { media.currentTimeMs }
    awaitCondition("three more seconds of playback or the end") {
      statuses.contains(VlcStatus.ENDED) || controller.read { media.currentTimeMs } >= start + BACKGROUND_PLAY_MS
    }
    assertFalse("ended with the video off; statuses=$statuses", statuses.contains(VlcStatus.ENDED))
    assertEquals(VlcStatus.PLAYING, controller.read { status })
  }

  @Test
  fun playsLocalFileAndEmitsLoadedOnceAfterPlaying() {
    controller.setSource(localSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    awaitCondition("loaded") { loaded.isNotEmpty() }

    assertEquals(VlcStatus.OPENING, statuses.first())
    assertTrue(statuses.indexOf(VlcStatus.OPENING) < statuses.indexOf(VlcStatus.PLAYING))
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
  fun loadedComesRightAfterTheFirstPlaying() {
    val order = CopyOnWriteArrayList<String>()
    controller.events.statusChange.add { order.add("status:$it") }
    controller.events.tracksChange.add { order.add("tracks") }
    controller.events.videoSizeChange.add { order.add("videoSize") }
    controller.events.loaded.add { order.add("loaded") }

    controller.setSource(tenSecondSource())
    awaitCondition("loaded") { order.contains("loaded") }

    assertEquals("order=$order", "loaded", order[order.indexOf("status:${VlcStatus.PLAYING}") + 1])
  }

  @Test
  fun seekEmitsATimeUpdateWithTheTargetTime() {
    controller.setLoop(true)
    controller.setSource(localSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    controller.seek(SEEK_TARGET_MS.toDouble())
    awaitCondition("seek time") { times.any { it.currentTime == SEEK_TARGET_MS.toDouble() } }
  }

  @Test
  fun invalidUriFailsWithInvalidSource() {
    controller.setSource(TestMedia.source("relative/path.mp4"))
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
  fun addedAudioArrivesThroughTracksChangeAndIsSelected() {
    controller.setLoop(true)
    controller.setSource(localSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    controller.addAudio(TestMedia.copyToCache(TestMedia.AUDIO_ASSET).absolutePath, true)
    awaitCondition("external audio selected") { tracks.any(::selectsExternalAudio) }
  }

  @Test
  fun videoOffKeepsPlayingWhileExternalAudioIsSelected() {
    controller.setSource(tenSecondSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    controller.addAudio(TestMedia.copyToCache(TestMedia.AUDIO_ASSET).absolutePath, true)
    awaitCondition("external audio selected") { tracks.any(::selectsExternalAudio) }
    controller.setVideoTrackEnabled(false)
    assertKeepsPlaying()
  }

  @Test
  fun selectingExternalAudioWithVideoOffKeepsPlaying() {
    controller.setSource(tenSecondSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    controller.setVideoTrackEnabled(false)
    controller.addAudio(TestMedia.copyToCache(TestMedia.AUDIO_ASSET).absolutePath, true)
    awaitCondition("external audio selected with the video off") { tracks.any(::selectsExternalAudio) }
    assertKeepsPlaying()
  }

  @Test
  fun addAudioWithoutSourceThrows() {
    try {
      controller.addAudio("file:///nothing.m4a", true)
      fail("expected addAudio to throw")
    } catch (error: VlcPlayerError) {
      assertEquals("addAudio requires a source", error.message)
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
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    assertEquals(countAfterRelease, statuses.size)
    assertEquals(VlcStatus.IDLE, controller.read { status })
    assertEquals(100, controller.read { settings.volumeControl.volume })
    assertNull(controller.read { source })
  }

  @Test
  fun subtitleStyleIsNormalisedImmediately() {
    controller.setSubtitleStyle(subtitleStyle(fontScale = 5.0, color = "#abc", backgroundColor = "bad"))
    val stored = controller.read { settings.subtitleStyle }
    assertEquals(25.0, stored?.fontScale)
    assertEquals("#AABBCC", stored?.color)
    assertNull(stored?.backgroundColor)
    controller.setSubtitleStyle(subtitleStyle())
    assertNull(controller.read { settings.subtitleStyle })
  }

  @Test
  fun subtitleStyleSetBeforeLoadReachesPlaying() {
    controller.setSubtitleStyle(YELLOW_SUBTITLES)
    controller.setSource(localSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    assertEquals(1, loaded.size)
    assertEquals(YELLOW_SUBTITLES.fontScale, controller.read { settings.subtitleStyle }?.fontScale)
  }

  @Test
  fun subtitleStyleChangeWhilePlayingReopensAndRestoresPositionAndSubtitle() {
    controller.setLoop(true)
    controller.setSource(localSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    controller.addSubtitle(TestMedia.copyToCache(TestMedia.SUBTITLE_ASSET).absolutePath, true)
    awaitCondition("subtitle selected") { tracks.any { it.subtitleTracks.isNotEmpty() && it.selectedSubtitleTrack >= 0 } }
    controller.seek(SEEK_TARGET_MS.toDouble())
    statuses.clear()
    tracks.clear()
    controller.setSubtitleStyle(YELLOW_SUBTITLES)
    awaitCondition("playing again") { statuses.contains(VlcStatus.PLAYING) }
    awaitCondition("position restored") { controller.read { media.currentTimeMs } >= SEEK_TARGET_MS - POSITION_TOLERANCE_MS }
    awaitCondition("subtitle restored") {
      controller.read { media.tracks.subtitleTracks.isNotEmpty() && media.tracks.selectedSubtitleTrack >= 0 }
    }
    assertEquals(VlcStatus.OPENING, statuses.first())
    assertEquals(1, loaded.size)
  }

  @Test
  fun subtitleStyleChangeWhilePlayingReattachesExternalAudio() {
    controller.setSource(tenSecondSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    controller.addAudio(TestMedia.copyToCache(TestMedia.AUDIO_ASSET).absolutePath, true)
    awaitCondition("external audio selected") { tracks.any(::selectsExternalAudio) }
    statuses.clear()
    tracks.clear()
    controller.setSubtitleStyle(YELLOW_SUBTITLES)
    awaitCondition("playing again") { statuses.contains(VlcStatus.PLAYING) }
    awaitCondition("external audio selected again after the reload") {
      val reload = tracks.indexOfFirst { it.audioTracks.size < 2 }
      reload >= 0 && tracks.drop(reload).any(::selectsExternalAudio)
    }
    assertEquals(1, loaded.size)
  }

  @Test
  fun subtitleStyleChangeWhilePausedStaysPaused() {
    controller.setLoop(true)
    controller.setSource(localSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    controller.seek(SEEK_TARGET_MS.toDouble())
    controller.pause()
    awaitCondition("paused") { controller.read { status } == VlcStatus.PAUSED }
    statuses.clear()
    controller.setSubtitleStyle(YELLOW_SUBTITLES)
    awaitCondition("paused again") { statuses.contains(VlcStatus.PLAYING) && controller.read { status } == VlcStatus.PAUSED }
    assertEquals(VlcStatus.OPENING, statuses.first())
    assertTrue(controller.read { media.currentTimeMs } >= SEEK_TARGET_MS - POSITION_TOLERANCE_MS)
  }

  @Test
  fun twoQuickSubtitleStyleChangesStillRestorePosition() {
    controller.setLoop(true)
    controller.setSource(localSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    controller.seek(SEEK_TARGET_MS.toDouble())
    statuses.clear()
    controller.setSubtitleStyle(YELLOW_SUBTITLES)
    controller.setSubtitleStyle(subtitleStyle(fontScale = 50.0, color = "#00FFFF"))
    awaitCondition("playing again") { statuses.contains(VlcStatus.PLAYING) }
    awaitCondition("position restored") { controller.read { media.currentTimeMs } >= SEEK_TARGET_MS - POSITION_TOLERANCE_MS }
    assertEquals(50.0, controller.read { settings.subtitleStyle?.fontScale })
  }

  @Test
  fun emitsBufferingPercentWithoutRepeats() {
    controller.setSource(localSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    awaitCondition("buffering percent") { bufferings.isNotEmpty() }

    assertTrue(bufferings.all { it.percent in 0.0..100.0 && it.percent == Math.rint(it.percent) })
    bufferings.zipWithNext().forEach { (previous, next) -> assertFalse("repeated ${next.percent}", previous.percent == next.percent) }
  }

  @Test
  fun libvlcOptionsChangeWhilePlayingReopensAndRestoresPosition() {
    controller.setLoop(true)
    controller.setSource(localSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    controller.seek(SEEK_TARGET_MS.toDouble())
    statuses.clear()
    controller.setLibvlcOptions(listOf("--network-caching=1500"))
    awaitCondition("playing again") { statuses.contains(VlcStatus.PLAYING) }
    awaitCondition("position restored") { controller.read { media.currentTimeMs } >= SEEK_TARGET_MS - POSITION_TOLERANCE_MS }
    assertEquals(VlcStatus.OPENING, statuses.first())
    assertEquals(listOf("--network-caching=1500"), controller.read { settings.libvlcOptions })
    assertEquals(1, loaded.size)
  }

  @Test
  fun rejectedLibvlcOptionKeepsThePlayerUsable() {
    controller.setLibvlcOptions(listOf(REJECTED_INSTANCE_OPTION))
    controller.setSource(localSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    assertTrue(errors.isEmpty())
  }

  @Test
  fun playAfterStoppingAnInvalidSourceFailsAgain() {
    controller.setSource(TestMedia.source(INVALID_URI))
    awaitCondition("error") { statuses.contains(VlcStatus.ERROR) }
    controller.stop()
    awaitCondition("stopped") { statuses.contains(VlcStatus.STOPPED) }
    statuses.clear()

    controller.play()

    awaitCondition("error again") { statuses.contains(VlcStatus.ERROR) }
    assertEquals(listOf(VlcErrorCode.INVALIDSOURCE, VlcErrorCode.INVALIDSOURCE), errors.map { it.code })
  }

  @Test
  fun durationStaysKnownWhileAStyleChangeReloads() {
    controller.setSource(tenSecondSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    awaitCondition("duration") { controller.read { media.durationMs } > 0L }
    statuses.clear()
    val durations = mutableSetOf<Long>()

    controller.setSubtitleStyle(YELLOW_SUBTITLES)
    awaitCondition("playing again") {
      durations.add(controller.read { media.durationMs })
      statuses.contains(VlcStatus.PLAYING)
    }

    assertFalse("durations=$durations", durations.contains(0L))
  }

  @Test
  fun seekDuringAStyleReloadIsKept() {
    controller.setSource(tenSecondSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    controller.seek(LATE_SEEK_MS.toDouble())
    awaitCondition("late position") { controller.read { media.currentTimeMs } >= LATE_SEEK_MS - POSITION_TOLERANCE_MS }
    statuses.clear()
    controller.setSubtitleStyle(YELLOW_SUBTITLES)
    awaitCondition("reopening") { statuses.contains(VlcStatus.OPENING) }

    controller.seek(EARLY_SEEK_MS.toDouble())
    awaitCondition("playing again") { statuses.contains(VlcStatus.PLAYING) }
    times.clear()
    awaitCondition("time moves") { times.size >= SETTLED_TIME_UPDATES }

    assertTrue(controller.read { media.currentTimeMs } < SEEK_MIDPOINT_MS)
  }

  @Test
  fun backwardSeekAfterAResumeDoesNotReload() {
    controller.setReloadOnStallAfter(STALL_CHECK_MS.toDouble())
    controller.setSource(tenSecondSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    controller.seek(LATE_SEEK_MS.toDouble())
    controller.pause()
    awaitCondition("paused") { statuses.last() == VlcStatus.PAUSED }
    statuses.clear()
    controller.play()
    awaitCondition("playing again") { statuses.contains(VlcStatus.PLAYING) }
    statuses.clear()

    controller.seek(EARLY_SEEK_MS.toDouble())
    awaitCondition("a reload or playback past the stall check") {
      statuses.contains(VlcStatus.OPENING) || controller.read { media.currentTimeMs } in PAST_THE_STALL_CHECK_MS until LATE_SEEK_MS
    }

    assertFalse("reloaded as a stall; statuses=$statuses", statuses.contains(VlcStatus.OPENING))
  }

  @Test
  fun snapshotSettlesWhenThePlayerIsReleasedFirst() {
    val mainThreadMayContinue = CountDownLatch(1)
    val settled = CountDownLatch(1)
    Handler(Looper.getMainLooper()).post { mainThreadMayContinue.await() }

    controller.captureSnapshot { settled.countDown() }
    controller.release()
    mainThreadMayContinue.countDown()

    assertTrue(settled.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
  }

  @Test
  fun playsAFileWhosePathHasAHashAndAPercentSign() {
    val original = TestMedia.copyToCache(TestMedia.VIDEO_ASSET)
    val renamed = original.copyTo(File(original.parentFile, FILE_NAME_WITH_URI_CHARACTERS), overwrite = true)

    controller.setSource(TestMedia.source(renamed.absolutePath))

    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
  }

  @Test
  fun aSubtitleAddedRightAfterTheSourceIsAttachedOnce() {
    controller.setSource(tenSecondSource())
    controller.addSubtitle(TestMedia.copyToCache(TestMedia.SUBTITLE_ASSET).absolutePath, true)
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    val start = controller.read { media.currentTimeMs }

    awaitCondition("playback past the slave reattach delay") { controller.read { media.currentTimeMs } >= start + PAST_THE_REATTACH_MS }

    assertEquals(1, controller.read { media.tracks.subtitleTracks.size })
  }

  @Test
  fun aSubtitleTurnedOffDuringAStyleReloadStaysOff() {
    controller.setSource(tenSecondSource())
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    controller.addSubtitle(TestMedia.copyToCache(TestMedia.SUBTITLE_ASSET).absolutePath, true)
    awaitCondition("subtitle selected") { controller.read { media.tracks.selectedSubtitleTrack >= 0 } }
    statuses.clear()
    controller.setSubtitleStyle(YELLOW_SUBTITLES)
    awaitCondition("reopening") { statuses.contains(VlcStatus.OPENING) }

    controller.setSubtitleTrack(VlcContract.NO_TRACK_ID.toDouble())
    awaitCondition("playing again") { statuses.contains(VlcStatus.PLAYING) }
    val start = controller.read { media.currentTimeMs }
    awaitCondition("playback past the slave reattach delay") { controller.read { media.currentTimeMs } >= start + PAST_THE_REATTACH_MS }

    assertEquals(VlcContract.NO_TRACK_ID.toDouble(), controller.read { media.tracks.selectedSubtitleTrack }, 0.0)
  }

  @Test
  fun playAfterAnErrorKeepsAnAddedSubtitle() {
    val missing = TestMedia.copyToCache(TestMedia.VIDEO_ASSET, RETRY_FOLDER)
    missing.delete()
    controller.setSource(TestMedia.source(missing.absolutePath))
    awaitCondition("error") { statuses.contains(VlcStatus.ERROR) }
    controller.addSubtitle(TestMedia.copyToCache(TestMedia.SUBTITLE_ASSET).absolutePath, true)
    TestMedia.copyToCache(TestMedia.VIDEO_ASSET, RETRY_FOLDER)
    statuses.clear()

    controller.play()

    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
    awaitCondition("subtitle listed") { controller.read { media.tracks.subtitleTracks.isNotEmpty() } }
  }

  private fun subtitleStyle(
    fontScale: Double? = null,
    color: String? = null,
    backgroundColor: String? = null,
    outlineThickness: VlcOutlineThickness? = null
  ) = VlcSubtitleStyle(
    fontFamily = null,
    fontScale = fontScale,
    bold = null,
    color = color,
    backgroundColor = backgroundColor,
    outlineColor = null,
    outlineThickness = outlineThickness,
    shadowColor = null,
    shadowDistance = null,
    marginBottom = null
  )

  private fun assertNoRepeatedStatus() {
    statuses.zipWithNext().forEach { (previous, next) -> assertFalse("repeated $next", previous == next) }
  }

  private fun selectsExternalAudio(info: VlcTracksInfo): Boolean =
    info.audioTracks.size == 2 && info.selectedAudioTrack == info.audioTracks.last().id

  private fun awaitCondition(label: String, condition: () -> Boolean) =
    awaitCondition(label, context = { "statuses=$statuses" }, condition = condition)

  private companion object {
    const val TIMEOUT_SECONDS = AWAIT_TIMEOUT_SECONDS
    const val BACKGROUND_PLAY_MS = 3_000L
    const val FAST_TIME_UPDATE_MS = 50.0
    const val SEEK_TARGET_MS = 1000L
    const val POSITION_TOLERANCE_MS = 300L
    const val REJECTED_INSTANCE_OPTION = "--no-such-libvlc-option"
    const val INVALID_URI = "not a uri"
    const val LATE_SEEK_MS = 6000L
    const val EARLY_SEEK_MS = 1000L
    const val SEEK_MIDPOINT_MS = 3500L
    const val SETTLED_TIME_UPDATES = 3
    const val STALL_CHECK_MS = 3000L
    const val PAST_THE_STALL_CHECK_MS = EARLY_SEEK_MS + STALL_CHECK_MS + 500L
    const val FILE_NAME_WITH_URI_CHARACTERS = "episode #1 100%.mp4"
    const val RETRY_FOLDER = "retry"
    const val PAST_THE_REATTACH_MS = 2 * VlcContract.SLAVE_REATTACH_DELAY_MS
    val YELLOW_SUBTITLES = VlcSubtitleStyle(
      fontFamily = null,
      fontScale = 150.0,
      bold = true,
      color = "#FFFF00",
      backgroundColor = null,
      outlineColor = null,
      outlineThickness = VlcOutlineThickness.THICK,
      shadowColor = null,
      shadowDistance = null,
      marginBottom = null
    )
  }
}
