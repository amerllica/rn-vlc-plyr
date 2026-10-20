package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcStatus

enum class MediaSessionPlayback { NONE, BUFFERING, PLAYING, PAUSED, STOPPED, ERROR }

enum class RemoteCommand { PLAY, PAUSE, TOGGLE_PLAY_PAUSE, STOP, SEEK, SKIP_FORWARD, SKIP_BACKWARD, NEXT, PREVIOUS }

data class MediaSessionState(
  val isActive: Boolean,
  val playback: MediaSessionPlayback,
  val positionMs: Long,
  val durationMs: Long,
  val playbackRate: Double,
  val commands: Set<RemoteCommand>
) {
  companion object {
    private val ALWAYS_AVAILABLE = setOf(RemoteCommand.PLAY, RemoteCommand.PAUSE, RemoteCommand.TOGGLE_PLAY_PAUSE, RemoteCommand.STOP)
    private val SEEKING = setOf(RemoteCommand.SEEK, RemoteCommand.SKIP_FORWARD, RemoteCommand.SKIP_BACKWARD)

    fun from(
      hasNowPlaying: Boolean,
      hasSource: Boolean,
      status: VlcStatus,
      currentTimeMs: Long,
      durationMs: Long,
      rate: Double,
      isSeekable: Boolean,
      isLive: Boolean,
      hasNextListener: Boolean,
      hasPreviousListener: Boolean
    ): MediaSessionState {
      val playback = playbackOf(status)
      return MediaSessionState(
        isActive = hasNowPlaying && hasSource,
        playback = playback,
        positionMs = if (isLive) 0L else currentTimeMs,
        durationMs = if (isLive) 0L else durationMs.coerceAtLeast(0L),
        playbackRate = if (playback == MediaSessionPlayback.PLAYING) rate else 0.0,
        commands = commandsFor(isSeekable && !isLive, hasNextListener, hasPreviousListener)
      )
    }

    private fun playbackOf(status: VlcStatus): MediaSessionPlayback = when (status) {
      VlcStatus.IDLE -> MediaSessionPlayback.NONE
      VlcStatus.OPENING, VlcStatus.BUFFERING -> MediaSessionPlayback.BUFFERING
      VlcStatus.PLAYING -> MediaSessionPlayback.PLAYING
      VlcStatus.PAUSED -> MediaSessionPlayback.PAUSED
      VlcStatus.STOPPED, VlcStatus.ENDED -> MediaSessionPlayback.STOPPED
      VlcStatus.ERROR -> MediaSessionPlayback.ERROR
    }

    private fun commandsFor(canSeek: Boolean, hasNext: Boolean, hasPrevious: Boolean): Set<RemoteCommand> = buildSet {
      addAll(ALWAYS_AVAILABLE)
      if (canSeek) addAll(SEEKING)
      if (hasNext) add(RemoteCommand.NEXT)
      if (hasPrevious) add(RemoteCommand.PREVIOUS)
    }
  }
}
