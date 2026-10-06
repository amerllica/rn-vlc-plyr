package com.margelo.nitro.rnvlcplyr

import com.margelo.nitro.rnvlcplyr.contract.MediaState
import com.margelo.nitro.rnvlcplyr.contract.PlayerSettings

class PlayerReadModel(
  val settings: PlayerSettings,
  val status: VlcStatus,
  val media: MediaState,
  val source: VlcSource?
)
