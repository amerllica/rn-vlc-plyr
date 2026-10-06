package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcError
import com.margelo.nitro.rnvlcplyr.VlcErrorCode
import com.margelo.nitro.rnvlcplyr.VlcSource
import kotlin.math.floor

object VlcContract {
  const val DEFAULT_VOLUME = 100
  const val DEFAULT_RATE = 1.0
  const val DEFAULT_TIME_UPDATE_INTERVAL_MS = 250
  const val DEFAULT_DELAY_MS = 0
  const val NO_TRACK_ID = -1

  private const val MIN_VOLUME = 0
  private const val MAX_VOLUME = 100
  private const val MIN_RATE = 0.25
  private const val MAX_RATE = 4.0
  private const val MIN_TIME_UPDATE_INTERVAL_MS = 50
  private const val MAX_TIME_UPDATE_INTERVAL_MS = 5000
  private const val MAX_DELAY_MS = 60_000
  private const val MICROS_PER_MILLI = 1000L
  private const val FILE_SCHEME_PREFIX = "file://"
  private const val USER_AGENT_OPTION = ":http-user-agent="
  private const val REFERRER_OPTION = ":http-referrer="

  private val SCHEME_PATTERN = Regex("^([A-Za-z][A-Za-z0-9+.-]*):")
  private val NETWORK_SCHEMES = setOf("http", "https", "rtsp", "rtmp", "rtp", "udp", "mms", "ftp", "smb")

  val INVALID_SOURCE_ERROR = VlcError(VlcErrorCode.INVALIDSOURCE, "Invalid source uri")
  val NETWORK_ERROR = VlcError(VlcErrorCode.NETWORK, "Could not open the media over the network")
  val MEDIA_ERROR = VlcError(VlcErrorCode.MEDIA, "The media could not be played")

  fun roundHalfUp(value: Double): Long = if (value.isNaN()) 0L else floor(value + 0.5).toLong()

  fun clampVolume(value: Double): Int = roundHalfUp(value).coerceIn(MIN_VOLUME.toLong(), MAX_VOLUME.toLong()).toInt()

  fun clampRate(value: Double): Double = if (value.isNaN()) DEFAULT_RATE else value.coerceIn(MIN_RATE, MAX_RATE)

  fun clampTimeUpdateInterval(value: Double): Int =
    roundHalfUp(value)
      .coerceIn(MIN_TIME_UPDATE_INTERVAL_MS.toLong(), MAX_TIME_UPDATE_INTERVAL_MS.toLong())
      .toInt()

  fun clampDelay(value: Double): Int = roundHalfUp(value).coerceIn(-MAX_DELAY_MS.toLong(), MAX_DELAY_MS.toLong()).toInt()

  fun delayToMicros(delayMs: Int): Long = delayMs * MICROS_PER_MILLI

  fun clampSeek(targetMs: Double, durationMs: Long): Long {
    val lowerBounded = roundHalfUp(targetMs).coerceAtLeast(0L)
    return if (durationMs > 0) lowerBounded.coerceAtMost(durationMs) else lowerBounded
  }

  fun durationFromEngine(lengthMs: Long): Long = if (lengthMs <= 0) 0L else lengthMs

  fun resolveMediaUri(uri: String): String? = when {
    uri.startsWith("/") -> FILE_SCHEME_PREFIX + uri
    schemeOf(uri) != null -> uri
    else -> null
  }

  fun mediaOptions(source: VlcSource): List<String> =
    listOfNotNull(
      source.userAgent?.let { USER_AGENT_OPTION + it },
      source.referrer?.let { REFERRER_OPTION + it }
    ) + source.vlcOptions.orEmpty()

  fun classifyEngineError(uri: String?, hasReachedPlaying: Boolean): VlcError {
    val scheme = uri?.let(::schemeOf)?.lowercase()
    val isNetworkSource = scheme != null && scheme in NETWORK_SCHEMES
    return if (isNetworkSource && !hasReachedPlaying) NETWORK_ERROR else MEDIA_ERROR
  }

  private fun schemeOf(uri: String): String? = SCHEME_PATTERN.find(uri)?.groupValues?.get(1)
}
