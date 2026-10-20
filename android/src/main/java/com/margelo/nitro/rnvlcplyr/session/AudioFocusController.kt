package com.margelo.nitro.rnvlcplyr.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.core.content.ContextCompat

class AudioFocusController(
  private val context: Context,
  private val onFocusChange: (Int) -> Unit,
  private val onBecomingNoisy: () -> Unit
) {
  private val noisyReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) onBecomingNoisy()
    }
  }
  private var listeningForNoisy = false

  fun request(): Boolean = SharedAudioFocus.request(context, onFocusChange)

  fun abandon() {
    SharedAudioFocus.abandon(context, onFocusChange)
  }

  fun listenForNoisy(enabled: Boolean) {
    if (enabled == listeningForNoisy) return
    listeningForNoisy = enabled
    if (enabled) {
      ContextCompat.registerReceiver(context, noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
    } else {
      context.unregisterReceiver(noisyReceiver)
    }
  }

  fun release() {
    listenForNoisy(false)
    abandon()
  }
}
