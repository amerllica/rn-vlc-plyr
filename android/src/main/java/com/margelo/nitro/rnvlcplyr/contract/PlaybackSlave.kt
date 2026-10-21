package com.margelo.nitro.rnvlcplyr.contract

data class PlaybackSlave(val kind: Kind, val uri: String, val select: Boolean) {
  enum class Kind { AUDIO, SUBTITLE }
}
