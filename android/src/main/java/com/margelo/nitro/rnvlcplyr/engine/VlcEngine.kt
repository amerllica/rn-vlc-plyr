package com.margelo.nitro.rnvlcplyr.engine

import android.content.Context
import android.util.Log
import com.margelo.nitro.NitroModules
import org.videolan.libvlc.LibVLC

object VlcEngine {
  private const val TAG = "RnVlcPlyr"
  private const val REJECTED_OPTIONS_WARNING = "rn-vlc-plyr: libVLC rejected the instance options, so the player uses the shared instance"

  @Volatile
  private var shared: LibVLC? = null

  fun applicationContext(): Context =
    NitroModules.applicationContext?.applicationContext
      ?: throw Error("rn-vlc-plyr: the React application context is not available yet")

  fun create(context: Context, options: List<String>): LibVLC? =
    runCatching { LibVLC(context.applicationContext, ArrayList(options)) }
      .onFailure { Log.w(TAG, REJECTED_OPTIONS_WARNING, it) }
      .getOrNull()

  fun libVLC(context: Context = applicationContext()): LibVLC =
    shared ?: synchronized(this) {
      shared ?: LibVLC(context.applicationContext, arrayListOf()).also { shared = it }
    }
}
