package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcStatus

val VlcStatus.isActive: Boolean
  get() = this == VlcStatus.OPENING || this == VlcStatus.BUFFERING || this == VlcStatus.PLAYING
