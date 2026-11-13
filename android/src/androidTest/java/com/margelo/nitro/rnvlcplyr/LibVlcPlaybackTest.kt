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

    try {
      assertTrue("player never reached Playing", playing.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
      awaitCondition("a length above zero", context = { "length=${player.length}" }) { player.length > 0 }
    } finally {
      player.stop()
      player.setEventListener(null)
      player.release()
    }
  }
}
