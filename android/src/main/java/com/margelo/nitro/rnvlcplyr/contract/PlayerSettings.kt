package com.margelo.nitro.rnvlcplyr.contract

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

  fun setAudioDelay(value: Double) {
    if (value.isNaN()) return
    audioDelayMs = VlcContract.clampDelay(value)
  }
}
