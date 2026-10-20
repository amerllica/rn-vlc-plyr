package com.margelo.nitro.rnvlcplyr.session

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.margelo.nitro.rnvlcplyr.contract.ArtworkRetrySchedule
import java.io.File
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

typealias ArtworkScheduler = (delayMs: Long, work: Runnable) -> Unit

class ArtworkLoader(private val schedule: ArtworkScheduler = defaultScheduler()) {
  private class Cycle(val context: Context) {
    val retries = ArtworkRetrySchedule()
    val waiters = LinkedHashSet<() -> Unit>()
  }

  private class ArtworkFailure(val reason: String) : Exception(reason)

  private val lock = Any()
  private var cache: Pair<String, Bitmap>? = null
  private val failed = mutableSetOf<String>()
  private val cycles = mutableMapOf<String, Cycle>()

  fun cached(uri: String): Bitmap? = synchronized(lock) { cache?.takeIf { it.first == uri }?.second }

  fun image(context: Context, uri: String, onSettled: () -> Unit): Bitmap? {
    synchronized(lock) {
      cache?.takeIf { it.first == uri }?.let { return it.second }
      if (uri in failed) return null
      cycles[uri]?.let { cycle ->
        cycle.waiters += onSettled
        return null
      }
      cycles[uri] = Cycle(context.applicationContext).apply { waiters += onSettled }
    }
    scheduleNextAttempt(uri)
    return null
  }

  fun reset(uri: String) {
    synchronized(lock) {
      failed.remove(uri)
      cycles[uri]?.retries?.reset()
    }
  }

  private fun scheduleNextAttempt(uri: String): Boolean {
    val delayMs = synchronized(lock) { cycles[uri]?.retries?.nextDelayMs() } ?: return false
    schedule(delayMs.toLong(), Runnable { attempt(uri) })
    return true
  }

  private fun attempt(uri: String) {
    val context = synchronized(lock) { cycles[uri]?.context } ?: return
    runCatching { decode(read(context, uri)) }
      .onSuccess { bitmap -> succeed(uri, bitmap) }
      .onFailure { error -> if (!scheduleNextAttempt(uri)) giveUp(uri, reasonFor(error)) }
  }

  private fun succeed(uri: String, bitmap: Bitmap) {
    val waiters = synchronized(lock) {
      cache = uri to bitmap
      cycles.remove(uri)?.waiters.orEmpty()
    }
    waiters.forEach { it() }
  }

  private fun giveUp(uri: String, reason: String) {
    val waiters = synchronized(lock) {
      failed += uri
      cycles.remove(uri)?.waiters.orEmpty()
    }
    Log.w(TAG, "$TAG: artwork could not be loaded from $uri: $reason")
    waiters.forEach { it() }
  }

  private fun reasonFor(error: Throwable): String = when (error) {
    is ArtworkFailure -> error.reason
    is SocketTimeoutException -> "timed out"
    is FileNotFoundException -> "file not found"
    else -> error.message ?: error.javaClass.simpleName
  }

  private fun read(context: Context, uri: String): ByteArray {
    val parsed = Uri.parse(uri)
    return when (parsed.scheme?.lowercase()) {
      "http", "https" -> readHttp(uri)
      "file" -> File(parsed.path ?: throw FileNotFoundException(uri)).readBytes()
      "content" -> context.contentResolver.openInputStream(parsed)?.use { it.readBytes() } ?: throw FileNotFoundException(uri)
      else -> throw ArtworkFailure("unsupported scheme")
    }
  }

  private fun readHttp(uri: String): ByteArray {
    val connection = URL(uri).openConnection() as HttpURLConnection
    connection.connectTimeout = TIMEOUT_MS
    connection.readTimeout = TIMEOUT_MS
    connection.instanceFollowRedirects = true
    return try {
      val status = connection.responseCode
      if (status !in HTTP_SUCCESS) throw ArtworkFailure("HTTP $status")
      connection.inputStream.use { it.readBytes() }
    } finally {
      connection.disconnect()
    }
  }

  private fun decode(bytes: ByteArray): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw ArtworkFailure(NOT_AN_IMAGE)
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(max(bounds.outWidth, bounds.outHeight)) }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: throw ArtworkFailure(NOT_AN_IMAGE)
    return fitWithinMaxSide(bitmap)
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

  companion object {
    private const val TAG = "rn-vlc-plyr"
    private const val MAX_SIDE_PX = 512
    private const val TIMEOUT_MS = 10_000
    private const val THREAD_NAME = "rn-vlc-plyr-artwork"
    private const val NOT_AN_IMAGE = "not an image"
    private val HTTP_SUCCESS = 200..299

    val shared by lazy { ArtworkLoader() }

    private fun defaultScheduler(): ArtworkScheduler {
      val executor = Executors.newSingleThreadScheduledExecutor { task -> Thread(task, THREAD_NAME).apply { isDaemon = true } }
      return { delayMs, work -> executor.schedule(work, delayMs, TimeUnit.MILLISECONDS) }
    }
  }
}
