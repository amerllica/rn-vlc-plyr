package com.margelo.nitro.rnvlcplyr

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubtitleRenderingTest {
  private lateinit var controller: PlayerController
  private lateinit var scenario: ActivityScenario<VideoHostActivity>
  private val statuses = CopyOnWriteArrayList<VlcStatus>()
  private val tracks = CopyOnWriteArrayList<VlcTracksInfo>()
  private var output: HostVideoOutput? = null

  @Before
  fun setUp() {
    TestMedia.warmLibVLC()
    warmSubtitleRendererOnce()
    controller = PlayerController(TestMedia.targetContext)
    controller.events.statusChange.add { statuses.add(it) }
    controller.events.tracksChange.add { tracks.add(it) }
    scenario = ActivityScenario.launch(VideoHostActivity::class.java)
  }

  @After
  fun tearDown() {
    controller.release()
    scenario.close()
  }

  @Test
  fun externalSubtitleDrawsOnTextureView() = assertExternalSubtitleDraws(usesTextureView = true, name = "external-texture")

  @Test
  fun externalSubtitleDrawsOnSurfaceView() = assertExternalSubtitleDraws(usesTextureView = false, name = "external-surface")

  @Test
  fun embeddedSubtitleDrawsOnTextureView() = assertEmbeddedSubtitleDraws(usesTextureView = true, name = "embedded-texture")

  @Test
  fun embeddedSubtitleDrawsOnSurfaceView() = assertEmbeddedSubtitleDraws(usesTextureView = false, name = "embedded-surface")

  @Test
  fun externalSubtitleDrawsAgainAfterStopAndPlay() {
    assertExternalSubtitleDraws(usesTextureView = true, name = "before-restart")
    controller.stop()
    awaitCondition("stopped") { statuses.last() == VlcStatus.STOPPED }
    statuses.clear()
    controller.play()
    awaitCondition("playing again") { statuses.contains(VlcStatus.PLAYING) }
    assertSubtitleOnScreen("after-restart")
  }

  @Test
  fun externalSubtitleDrawsAfterSwitchingToSurfaceViewWhilePlaying() {
    assertExternalSubtitleDraws(usesTextureView = true, name = "external-before-switch")
    switchSurfaceType(usesTextureView = false)
    assertSubtitleOnScreen("external-after-switch")
  }

  @Test
  fun embeddedSubtitleDrawsAfterSwitchingToTextureViewWhilePlaying() {
    assertEmbeddedSubtitleDraws(usesTextureView = false, name = "embedded-before-switch")
    switchSurfaceType(usesTextureView = true)
    assertSubtitleOnScreen("embedded-after-switch")
  }

  @Test
  fun externalSubtitleDrawsAfterTheVideoTrackComesBack() {
    assertExternalSubtitleDraws(usesTextureView = true, name = "before-video-off")
    controller.setVideoTrackEnabled(false)
    Thread.sleep(VIDEO_OFF_MS)
    controller.setVideoTrackEnabled(true)
    assertSubtitleOnScreen("after-video-on")
  }

  private fun switchSurfaceType(usesTextureView: Boolean) {
    scenario.onActivity {
      val shown = output ?: return@onActivity
      shown.useSurfaceType(usesTextureView)
      controller.renewOutput(shown)
    }
  }

  private fun assertExternalSubtitleDraws(usesTextureView: Boolean, name: String) {
    play(TestMedia.PLAIN_VIDEO_ASSET, usesTextureView)
    controller.addSubtitle(TestMedia.copyToCache(TestMedia.LONG_CUE_SUBTITLE_ASSET, SUBTITLE_FOLDER).absolutePath, true)
    assertSubtitleOnScreen(name)
  }

  private fun assertEmbeddedSubtitleDraws(usesTextureView: Boolean, name: String) {
    play(TestMedia.EMBEDDED_SUBTITLE_ASSET, usesTextureView)
    awaitCondition("embedded subtitle track") { tracks.any { it.subtitleTracks.isNotEmpty() } }
    controller.setSubtitleTrack(tracks.first { it.subtitleTracks.isNotEmpty() }.subtitleTracks.first().id)
    assertSubtitleOnScreen(name)
  }

  private fun play(asset: String, usesTextureView: Boolean) {
    scenario.onActivity { activity ->
      output = activity.createOutput(usesTextureView).also(controller::attachOutput)
    }
    controller.setSource(VlcSource(TestMedia.copyToCache(asset).absolutePath, null, null, null, null))
    controller.play()
    awaitCondition("playing") { statuses.contains(VlcStatus.PLAYING) }
  }

  private fun assertSubtitleOnScreen(name: String) {
    val (subtitlePixels, screenshot) = waitForSubtitlePixels(SUBTITLE_TIMEOUT_SECONDS)
    screenshot?.let { save(it, name) }
    assertTrue("$name: $subtitlePixels subtitle pixels on screen; statuses=$statuses", subtitlePixels >= MIN_SUBTITLE_PIXELS)
  }

  private fun waitForSubtitlePixels(timeoutSeconds: Long): Pair<Int, Bitmap?> {
    val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds)
    var subtitlePixels = 0
    var screenshot: Bitmap? = null
    while (System.currentTimeMillis() < deadline && subtitlePixels < MIN_SUBTITLE_PIXELS) {
      Thread.sleep(SCREENSHOT_INTERVAL_MS)
      screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
      subtitlePixels = screenshot?.let(::countSubtitlePixels) ?: 0
    }
    return subtitlePixels to screenshot
  }

  private fun warmSubtitleRendererOnce() {
    if (subtitleRendererWarm) return
    val warmController = PlayerController(TestMedia.targetContext)
    val warmScenario = ActivityScenario.launch(VideoHostActivity::class.java)
    try {
      warmScenario.onActivity { activity -> warmController.attachOutput(activity.createOutput(usesTextureView = true)) }
      warmController.setLoop(true)
      warmController.setSource(VlcSource(TestMedia.copyToCache(TestMedia.PLAIN_VIDEO_ASSET).absolutePath, null, null, null, null))
      warmController.play()
      warmController.addSubtitle(TestMedia.copyToCache(TestMedia.LONG_CUE_SUBTITLE_ASSET, SUBTITLE_FOLDER).absolutePath, true)
      waitForSubtitlePixels(WARM_UP_SECONDS)
      subtitleRendererWarm = true
    } finally {
      warmController.release()
      warmScenario.close()
    }
  }

  private fun countSubtitlePixels(bitmap: Bitmap): Int {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.copy(Bitmap.Config.ARGB_8888, false).getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    return pixels.count(::isSubtitleColour)
  }

  private fun isSubtitleColour(pixel: Int): Boolean =
    Color.red(pixel) > CHANNEL_HIGH && Color.blue(pixel) > CHANNEL_HIGH && Color.green(pixel) < CHANNEL_LOW

  private fun save(bitmap: Bitmap, name: String) {
    val directory = TestMedia.targetContext.getExternalFilesDir(null) ?: return
    File(directory, "subtitle-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
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
    const val SUBTITLE_TIMEOUT_SECONDS = 6L
    const val WARM_UP_SECONDS = 30L
    const val POLL_MS = 50L
    const val SCREENSHOT_INTERVAL_MS = 250L
    const val VIDEO_OFF_MS = 1000L
    const val SUBTITLE_FOLDER = "subtitles"
    const val MIN_SUBTITLE_PIXELS = 200
    const val CHANNEL_HIGH = 190
    const val CHANNEL_LOW = 90

    @Volatile
    private var subtitleRendererWarm = false
  }
}
