package com.margelo.nitro.rnvlcplyr.engine

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.videolan.libvlc.MediaPlayer

object PlayerRetirement {
  private const val THREAD_NAME = "rn-vlc-plyr-retire"

  private val executor: ExecutorService = Executors.newCachedThreadPool { task ->
    Thread(task, THREAD_NAME).apply { isDaemon = true }
  }

  fun retire(player: MediaPlayer) = run {
    player.stop()
    player.release()
  }

  fun run(shutdown: () -> Unit) {
    executor.execute(shutdown)
  }
}
