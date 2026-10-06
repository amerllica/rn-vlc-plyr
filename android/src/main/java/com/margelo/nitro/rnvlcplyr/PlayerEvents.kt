package com.margelo.nitro.rnvlcplyr

import android.util.Log
import com.margelo.nitro.rnvlcplyr.contract.ListenerRegistry

class PlayerEvents {
  val statusChange = registry<VlcStatus>()
  val timeUpdate = registry<VlcTimeUpdate>()
  val loaded = registry<VlcLoadedInfo>()
  val ended = registry<Unit>()
  val error = registry<VlcError>()
  val volumeChange = registry<VlcVolumeInfo>()
  val tracksChange = registry<VlcTracksInfo>()
  val videoSizeChange = registry<VlcVideoSize>()

  private val all = listOf(statusChange, timeUpdate, loaded, ended, error, volumeChange, tracksChange, videoSizeChange)

  fun <T> subscribe(registry: ListenerRegistry<T>, listener: (T) -> Unit): VlcListenerSubscription =
    VlcListenerSubscription(registry.add(listener))

  fun clear() {
    all.forEach { it.clear() }
  }

  private fun <T> registry(): ListenerRegistry<T> = ListenerRegistry(::reportListenerFailure)

  private fun reportListenerFailure(failure: Throwable) {
    Log.e(TAG, "A VlcPlayer listener threw", failure)
  }

  private companion object {
    const val TAG = "RnVlcPlyr"
  }
}
