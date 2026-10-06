package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcError
import com.margelo.nitro.rnvlcplyr.VlcStatus

enum class EngineEvent { OPENING, BUFFERING, PLAYING, PAUSED, STOPPED, END_REACHED, ERROR }

enum class PlayAction { NONE, RESUME, RESTART, RELOAD }

sealed interface PlaybackEffect {
  data class StatusChanged(val status: VlcStatus) : PlaybackEffect
  data object Loaded : PlaybackEffect
  data object Ended : PlaybackEffect
  data class Failed(val error: VlcError) : PlaybackEffect
  data object RestartForLoop : PlaybackEffect
}

class PlaybackStateMachine(private val isLooping: () -> Boolean) {
  var status: VlcStatus = VlcStatus.IDLE
    private set
  var hasReachedPlaying: Boolean = false
    private set
  var hasSource: Boolean = false
    private set

  private var sourceUri: String? = null
  private var restartingForLoop = false

  fun clear(): List<PlaybackEffect> {
    hasSource = false
    sourceUri = null
    resetPerSourceFlags()
    return transition(VlcStatus.IDLE)
  }

  fun begin(uri: String, autoPlay: Boolean): List<PlaybackEffect> {
    hasSource = true
    sourceUri = uri
    resetPerSourceFlags()
    return transition(if (autoPlay) VlcStatus.OPENING else VlcStatus.IDLE)
  }

  fun rejectInvalidSource(): List<PlaybackEffect> {
    hasSource = true
    sourceUri = null
    resetPerSourceFlags()
    return fail(VlcContract.INVALID_SOURCE_ERROR)
  }

  fun resolvePlay(): PlayAction = when {
    !hasSource -> PlayAction.NONE
    status == VlcStatus.ENDED || status == VlcStatus.STOPPED -> PlayAction.RESTART
    status == VlcStatus.ERROR -> PlayAction.RELOAD
    else -> PlayAction.RESUME
  }

  fun canPause(): Boolean = status == VlcStatus.PLAYING || status == VlcStatus.BUFFERING

  fun seekRequiresRestart(): Boolean = status == VlcStatus.ENDED || status == VlcStatus.STOPPED

  fun isPlaybackActive(): Boolean = status in ACTIVE_STATUSES

  fun acceptsMediaInfo(): Boolean = hasSource && isPlaybackActive() && !restartingForLoop

  fun stop(): List<PlaybackEffect>? {
    if (status == VlcStatus.IDLE) return null
    restartingForLoop = false
    return transition(VlcStatus.STOPPED)
  }

  fun onEngineEvent(event: EngineEvent, engineIsPlaying: Boolean): List<PlaybackEffect> {
    if (sourceUri == null) return emptyList()
    return when (event) {
      EngineEvent.OPENING -> if (restartingForLoop) emptyList() else transition(VlcStatus.OPENING)
      EngineEvent.BUFFERING -> onBuffering(engineIsPlaying)
      EngineEvent.PLAYING -> onPlaying()
      EngineEvent.PAUSED -> transition(VlcStatus.PAUSED)
      EngineEvent.STOPPED -> emptyList()
      EngineEvent.END_REACHED -> onEndReached()
      EngineEvent.ERROR -> {
        restartingForLoop = false
        fail(VlcContract.classifyEngineError(sourceUri, hasReachedPlaying))
      }
    }
  }

  private fun onBuffering(engineIsPlaying: Boolean): List<PlaybackEffect> =
    if (restartingForLoop || engineIsPlaying || status == VlcStatus.PAUSED) emptyList()
    else transition(VlcStatus.BUFFERING)

  private fun onPlaying(): List<PlaybackEffect> {
    restartingForLoop = false
    val effects = transition(VlcStatus.PLAYING)
    if (hasReachedPlaying) return effects
    hasReachedPlaying = true
    return effects + PlaybackEffect.Loaded
  }

  private fun onEndReached(): List<PlaybackEffect> {
    if (isLooping()) {
      restartingForLoop = true
      return listOf(PlaybackEffect.RestartForLoop)
    }
    val effects = transition(VlcStatus.ENDED)
    return if (effects.isEmpty()) effects else effects + PlaybackEffect.Ended
  }

  private fun fail(error: VlcError): List<PlaybackEffect> {
    val effects = transition(VlcStatus.ERROR)
    return if (effects.isEmpty()) effects else effects + PlaybackEffect.Failed(error)
  }

  private fun transition(next: VlcStatus): List<PlaybackEffect> {
    if (next == status) return emptyList()
    status = next
    return listOf(PlaybackEffect.StatusChanged(next))
  }

  private fun resetPerSourceFlags() {
    hasReachedPlaying = false
    restartingForLoop = false
  }

  private companion object {
    val ACTIVE_STATUSES = setOf(VlcStatus.OPENING, VlcStatus.BUFFERING, VlcStatus.PLAYING, VlcStatus.PAUSED)
  }
}
