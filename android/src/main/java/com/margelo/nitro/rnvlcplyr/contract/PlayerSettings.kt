package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcAndroidNotification
import com.margelo.nitro.rnvlcplyr.VlcNowPlaying
import com.margelo.nitro.rnvlcplyr.VlcSubtitleStyle

class PlayerSettings {
  val volumeControl = VolumeControl()
  var rate: Double = VlcContract.DEFAULT_RATE
    private set
  var loop: Boolean = false
  var autoPlay: Boolean = true
  var timeUpdateIntervalMs: Int = VlcContract.DEFAULT_TIME_UPDATE_INTERVAL_MS
    private set
  var subtitleDelayMs: Int = VlcContract.DEFAULT_DELAY_MS
    private set
  var audioDelayMs: Int = VlcContract.DEFAULT_DELAY_MS
    private set
  var subtitleStyle: VlcSubtitleStyle? = null
    private set
  var staysActiveInBackground: Boolean = false
  var nowPlaying: VlcNowPlaying? = null
  var androidNotification: VlcAndroidNotification? = null
  var remoteSkipIntervalMs: Int = VlcContract.DEFAULT_REMOTE_SKIP_INTERVAL_MS
    private set

  fun setRate(value: Double) {
    if (value.isNaN()) return
    rate = VlcContract.clampRate(value)
  }

  fun setTimeUpdateInterval(value: Double) {
    if (value.isNaN()) return
    timeUpdateIntervalMs = VlcContract.clampTimeUpdateInterval(value)
  }

  fun setSubtitleDelay(value: Double) {
    if (value.isNaN()) return
    subtitleDelayMs = VlcContract.clampDelay(value)
  }

  fun setSubtitleStyle(value: VlcSubtitleStyle?): Boolean {
    val normalized = SubtitleStyles.normalize(value)
    if (normalized == subtitleStyle) return false
    subtitleStyle = normalized
    return true
  }

  fun setAudioDelay(value: Double) {
    if (value.isNaN()) return
    audioDelayMs = VlcContract.clampDelay(value)
  }

  fun setRemoteSkipInterval(value: Double) {
    if (value.isNaN()) return
    remoteSkipIntervalMs = VlcContract.clampSkipInterval(value)
  }
}
