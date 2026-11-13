package com.margelo.nitro.rnvlcplyr.contract

import android.media.AudioManager

enum class InterruptionEffect { PAUSE, RESUME }

class InterruptionPolicy {
  var pausedByInterruption: Boolean = false
    private set

  fun began(isActive: Boolean): InterruptionEffect? {
    if (!isActive) return null
    pausedByInterruption = true
    return InterruptionEffect.PAUSE
  }

  fun ended(shouldResume: Boolean): InterruptionEffect? {
    val resumes = pausedByInterruption && shouldResume
    pausedByInterruption = false
    return if (resumes) InterruptionEffect.RESUME else null
  }

  fun focusChanged(change: Int, isActive: Boolean): InterruptionEffect? = when (change) {
    AudioManager.AUDIOFOCUS_LOSS -> lostPermanently(isActive)
    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> began(isActive)
    AudioManager.AUDIOFOCUS_GAIN -> ended(shouldResume = true)
    else -> null
  }

  fun lostPermanently(isActive: Boolean): InterruptionEffect? = pauseAndForget(isActive)

  fun becameNoisy(isActive: Boolean): InterruptionEffect? = pauseAndForget(isActive)

  fun reset() {
    pausedByInterruption = false
  }

  private fun pauseAndForget(isActive: Boolean): InterruptionEffect? {
    pausedByInterruption = false
    return if (isActive) InterruptionEffect.PAUSE else null
  }
}
