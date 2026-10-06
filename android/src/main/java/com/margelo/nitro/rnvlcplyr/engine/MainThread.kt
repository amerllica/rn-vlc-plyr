package com.margelo.nitro.rnvlcplyr.engine

import android.os.Handler
import android.os.Looper

object MainThread {
  private val handler = Handler(Looper.getMainLooper())

  val isCurrent: Boolean
    get() = Looper.myLooper() == Looper.getMainLooper()

  fun run(block: () -> Unit) {
    if (isCurrent) block() else handler.post(block)
  }

  fun post(block: () -> Unit) {
    handler.post(block)
  }
}
