package com.margelo.nitro.rnvlcplyr.engine

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.util.concurrent.Executors

object SnapshotWriter {
  private const val PNG_QUALITY = 100
  private const val FILE_PREFIX = "vlc-snapshot-"
  private const val FILE_EXTENSION = ".png"

  private val executor = Executors.newSingleThreadExecutor()

  fun writePng(context: Context, bitmap: Bitmap, onDone: (Result<String>) -> Unit) {
    executor.execute {
      onDone(runCatching { writeToCache(context, bitmap) })
    }
  }

  private fun writeToCache(context: Context, bitmap: Bitmap): String {
    val file = File(context.cacheDir, FILE_PREFIX + System.currentTimeMillis() + FILE_EXTENSION)
    file.outputStream().use { stream ->
      check(bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, stream)) { "PNG encoding failed" }
    }
    bitmap.recycle()
    return Uri.fromFile(file).toString()
  }
}
