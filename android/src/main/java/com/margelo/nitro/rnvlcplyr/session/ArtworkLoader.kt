package com.margelo.nitro.rnvlcplyr.session

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.roundToInt

object ArtworkLoader {
  private const val MAX_SIDE_PX = 512
  private const val TIMEOUT_MS = 10_000
  private const val THREAD_NAME = "rn-vlc-plyr-artwork"
  private val HTTP_SUCCESS = 200..299

  private val executor = Executors.newSingleThreadExecutor { task -> Thread(task, THREAD_NAME).apply { isDaemon = true } }

  @Volatile
  private var cache: Pair<String, Bitmap>? = null

  fun cached(uri: String): Bitmap? = cache?.takeIf { it.first == uri }?.second

  fun load(context: Context, uri: String, onLoaded: (Bitmap?) -> Unit) {
    val appContext = context.applicationContext
    executor.execute {
      val bitmap = cached(uri) ?: runCatching { decode(read(appContext, uri)) }.getOrNull()
      if (bitmap != null) cache = uri to bitmap
      onLoaded(bitmap)
    }
  }

  private fun read(context: Context, uri: String): ByteArray? {
    val parsed = Uri.parse(uri)
    return when (parsed.scheme?.lowercase()) {
      "http", "https" -> readHttp(uri)
      "file" -> parsed.path?.let { File(it).readBytes() }
      "content" -> context.contentResolver.openInputStream(parsed)?.use { it.readBytes() }
      else -> null
    }
  }

  private fun readHttp(uri: String): ByteArray? {
    val connection = URL(uri).openConnection() as HttpURLConnection
    connection.connectTimeout = TIMEOUT_MS
    connection.readTimeout = TIMEOUT_MS
    connection.instanceFollowRedirects = true
    return try {
      if (connection.responseCode in HTTP_SUCCESS) connection.inputStream.use { it.readBytes() } else null
    } finally {
      connection.disconnect()
    }
  }

  private fun decode(bytes: ByteArray?): Bitmap? {
    if (bytes == null) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(max(bounds.outWidth, bounds.outHeight)) }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.let(::fitWithinMaxSide)
  }

  private fun sampleSize(longSide: Int): Int {
    var size = 1
    while (longSide / (size * 2) >= MAX_SIDE_PX) size *= 2
    return size
  }

  private fun fitWithinMaxSide(bitmap: Bitmap): Bitmap {
    val longSide = max(bitmap.width, bitmap.height)
    if (longSide <= MAX_SIDE_PX) return bitmap
    val scale = MAX_SIDE_PX.toFloat() / longSide
    val width = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
    val height = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, width, height, true)
  }
}
