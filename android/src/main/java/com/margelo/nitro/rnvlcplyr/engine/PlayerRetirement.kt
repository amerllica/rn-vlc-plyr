package com.margelo.nitro.rnvlcplyr.engine

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.videolan.libvlc.MediaPlayer

object PlayerRetirement {
  private const val THREAD_NAME = "rn-vlc-plyr-retire"

  private val executor: ExecutorService = Executors.newSingleThreadExecutor { task ->
    Thread(task, THREAD_NAME).apply { isDaemon = true }
  }

  fun retire(player: MediaPlayer) {
    executor.execute {
      player.stop()
      player.release()
    }
  }
}
