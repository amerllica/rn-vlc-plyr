package com.margelo.nitro.rnvlcplyr

class VlcPlayerError(message: String) : Error(message, null, false, false) {
  override fun toString(): String = message.orEmpty()
}
