package com.margelo.nitro.rnvlcplyr.contract

enum class BackgroundEffect { PAUSE, DISABLE_VIDEO, ENABLE_VIDEO }

class BackgroundPolicy {
  var inBackground: Boolean = false
    private set
  var videoDisabled: Boolean = false
    private set

  fun enterBackground(staysActive: Boolean, isActive: Boolean): List<BackgroundEffect> {
    if (inBackground) return emptyList()
    inBackground = true
    return when {
      staysActive -> disableVideo()
      isActive -> listOf(BackgroundEffect.PAUSE)
      else -> emptyList()
    }
  }

  fun enterForeground(): List<BackgroundEffect> {
    inBackground = false
    if (!videoDisabled) return emptyList()
    videoDisabled = false
    return listOf(BackgroundEffect.ENABLE_VIDEO)
  }

  fun staysActiveChanged(staysActive: Boolean, isActive: Boolean): List<BackgroundEffect> = when {
    !inBackground -> emptyList()
    !staysActive && isActive -> listOf(BackgroundEffect.PAUSE)
    staysActive && !videoDisabled -> disableVideo()
    else -> emptyList()
  }

  private fun disableVideo(): List<BackgroundEffect> {
    videoDisabled = true
    return listOf(BackgroundEffect.DISABLE_VIDEO)
  }
}
