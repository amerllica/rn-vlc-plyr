package com.margelo.nitro.rnvlcplyr

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

@RunWith(AndroidJUnit4::class)
class LibVlcPlaybackTest {
  @Test
  fun realMediaPlayerReachesPlayingAndReportsLength() {
    val file = TestMedia.copyToCache(TestMedia.VIDEO_ASSET)
    val libVLC = TestMedia.warmLibVLC()
    val player = MediaPlayer(libVLC)
    val playing = CountDownLatch(1)
    player.setEventListener(MediaPlayer.EventListener { event ->
      if (event.type == MediaPlayer.Event.Playing) playing.countDown()
    })
    val media = Media(libVLC, Uri.fromFile(file))
    player.media = media
    media.release()
    player.play()

    assertTrue("player never reached Playing", playing.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    assertTrue("length was ${player.length}", waitFor { player.length > 0 })

    player.stop()
    player.setEventListener(null)
    player.release()
  }

  private fun waitFor(condition: () -> Boolean): Boolean {
    val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS)
    while (System.currentTimeMillis() < deadline) {
      if (condition()) return true
      Thread.sleep(POLL_MS)
    }
    return false
  }

  private companion object {
    const val TIMEOUT_SECONDS = 15L
    const val POLL_MS = 50L
  }
}
