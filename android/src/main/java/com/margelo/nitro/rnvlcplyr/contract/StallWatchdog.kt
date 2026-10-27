package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcStatus

class StallWatchdog {
  private var armed: Arming? = null
  private var lastGeneration = 0

  fun arm(positionMs: Long): Int {
    lastGeneration += 1
    armed = Arming(lastGeneration, positionMs)
    return lastGeneration
  }

  fun disarm() {
    armed = null
  }

  fun fire(generation: Int, status: VlcStatus, positionMs: Long): Boolean {
    val current = armed?.takeIf { it.generation == generation } ?: return false
    armed = null
    return status == VlcStatus.BUFFERING || (status == VlcStatus.PLAYING && positionMs <= current.positionMs)
  }

  private data class Arming(val generation: Int, val positionMs: Long)
}
