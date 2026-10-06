package com.margelo.nitro.rnvlcplyr.engine

import android.content.Context
import com.margelo.nitro.NitroModules
import org.videolan.libvlc.LibVLC

object VlcEngine {
  private val LIBVLC_OPTIONS = arrayListOf<String>()

  @Volatile
  private var shared: LibVLC? = null

  fun applicationContext(): Context =
    NitroModules.applicationContext?.applicationContext
      ?: throw Error("rn-vlc-plyr: the React application context is not available yet")

  fun libVLC(context: Context = applicationContext()): LibVLC =
    shared ?: synchronized(this) {
      shared ?: LibVLC(context.applicationContext, LIBVLC_OPTIONS).also { shared = it }
    }
}
