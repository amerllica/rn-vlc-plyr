package com.margelo.nitro.rnvlcplyr.contract

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
