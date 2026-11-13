package com.margelo.nitro.rnvlcplyr.session

import android.media.AudioManager
import com.margelo.nitro.rnvlcplyr.contract.FocusChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusChangeTest {
  @Test
  fun lossBriefLossAndGainAreMapped() {
    assertEquals(FocusChange.LOST, focusChangeOf(AudioManager.AUDIOFOCUS_LOSS))
    assertEquals(FocusChange.LOST_BRIEFLY, focusChangeOf(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT))
    assertEquals(FocusChange.GAINED, focusChangeOf(AudioManager.AUDIOFOCUS_GAIN))
  }

  @Test
  fun duckingAndOtherChangesAreIgnored() {
    assertNull(focusChangeOf(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK))
    assertNull(focusChangeOf(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT))
  }
}
