package com.margelo.nitro.rnvlcplyr

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.margelo.nitro.rnvlcplyr.engine.VlcEngine
import org.videolan.libvlc.LibVLC
import java.io.File

object TestMedia {
  const val VIDEO_ASSET = "two-seconds.mp4"
  const val AUDIO_ASSET = "grey-ten-seconds.m4a"
  const val SUBTITLE_ASSET = "subtitles.srt"
  const val PLAIN_VIDEO_ASSET = "grey-ten-seconds.mp4"
  const val EMBEDDED_SUBTITLE_ASSET = "embedded-subtitle.mkv"
  const val LONG_CUE_SUBTITLE_ASSET = "subtitle-check.srt"
  const val VIDEO_WIDTH = 640.0
  const val VIDEO_HEIGHT = 360.0

  val targetContext: Context
    get() = InstrumentationRegistry.getInstrumentation().targetContext

  @Volatile
  private var fontCacheReady = false

  fun warmLibVLC(): LibVLC {
    val libVLC = VlcEngine.libVLC(targetContext)
    synchronized(this) {
      if (!fontCacheReady) {
        libVLC.buildFontCache()
        fontCacheReady = true
      }
    }
    return libVLC
  }

  fun copyToCache(asset: String, folder: String? = null): File {
    val directory = folder?.let { File(targetContext.cacheDir, it).apply { mkdirs() } } ?: targetContext.cacheDir
    val file = File(directory, asset)
    InstrumentationRegistry.getInstrumentation().context.assets.open(asset).use { input ->
      file.outputStream().use { output -> input.copyTo(output) }
    }
    return file
  }
}
