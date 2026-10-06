package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcVolumeInfo

class VolumeControl {
  var volume: Int = VlcContract.DEFAULT_VOLUME
    private set
  var muted: Boolean = false
    private set

  val engineVolume: Int
    get() = if (muted) MUTED_ENGINE_VOLUME else volume

  val info: VlcVolumeInfo
    get() = VlcVolumeInfo(volume.toDouble(), muted)

  fun setVolume(value: Double): Boolean {
    if (value.isNaN()) return false
    val clamped = VlcContract.clampVolume(value)
    if (clamped == volume) return false
    volume = clamped
    return true
  }

  fun setMuted(value: Boolean): Boolean {
    if (value == muted) return false
    muted = value
    return true
  }

  private companion object {
    const val MUTED_ENGINE_VOLUME = 0
  }
}
