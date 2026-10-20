package com.margelo.nitro.rnvlcplyr.session

import com.margelo.nitro.rnvlcplyr.PlayerEvents
import com.margelo.nitro.rnvlcplyr.PlayerReadModel
import com.margelo.nitro.rnvlcplyr.VlcAndroidNotification
import com.margelo.nitro.rnvlcplyr.VlcNowPlaying
import com.margelo.nitro.rnvlcplyr.VlcStatus
import com.margelo.nitro.rnvlcplyr.contract.MediaSessionState

interface SessionHost {
  val events: PlayerEvents

  fun sessionSnapshot(): SessionSnapshot
  fun play()
  fun pause()
  fun stop()
  fun seek(timeMs: Double)
  fun seekBy(deltaMs: Double)
  fun pauseForSession()
  fun setVideoTrackEnabled(enabled: Boolean)
}

data class SessionSnapshot(
  val nowPlaying: VlcNowPlaying?,
  val hasSource: Boolean,
  val status: VlcStatus,
  val currentTimeMs: Long,
  val durationMs: Long,
  val rate: Double,
  val isSeekable: Boolean,
  val isLive: Boolean,
  val hasNextListener: Boolean,
  val hasPreviousListener: Boolean,
  val staysActiveInBackground: Boolean,
  val remoteSkipIntervalMs: Int,
  val androidNotification: VlcAndroidNotification?
) {
  val sessionState: MediaSessionState
    get() = MediaSessionState.from(
      hasNowPlaying = nowPlaying != null,
      hasSource = hasSource,
      status = status,
      currentTimeMs = currentTimeMs,
      durationMs = durationMs,
      rate = rate,
      isSeekable = isSeekable,
      isLive = isLive,
      hasNextListener = hasNextListener,
      hasPreviousListener = hasPreviousListener
    )

  companion object {
    fun of(model: PlayerReadModel, hasNextListener: Boolean, hasPreviousListener: Boolean) = SessionSnapshot(
      nowPlaying = model.settings.nowPlaying,
      hasSource = model.source != null,
      status = model.status,
      currentTimeMs = model.media.currentTimeMs,
      durationMs = model.media.durationMs,
      rate = model.settings.rate,
      isSeekable = model.media.isSeekable,
      isLive = model.media.isLive,
      hasNextListener = hasNextListener,
      hasPreviousListener = hasPreviousListener,
      staysActiveInBackground = model.settings.staysActiveInBackground,
      remoteSkipIntervalMs = model.settings.remoteSkipIntervalMs,
      androidNotification = model.settings.androidNotification
    )
  }
}
