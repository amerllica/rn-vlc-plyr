package com.margelo.nitro.rnvlcplyr.session

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat

object SharedAudioFocus {
  private val lock = Any()
  private val holders = LinkedHashSet<(Int) -> Unit>()
  private var registered = false
  private var hasFocus = false
  private var request: AudioFocusRequestCompat? = null

  fun request(context: Context, holder: (Int) -> Unit): Boolean {
    var added = false
    val pending = synchronized(lock) {
      added = holders.add(holder)
      if (registered && hasFocus) return true
      focusRequest()
    }
    val granted = AudioManagerCompat.requestAudioFocus(audioManager(context), pending) ==
      AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    synchronized(lock) {
      if (granted) {
        registered = true
        hasFocus = true
      } else if (added) {
        holders.remove(holder)
      }
    }
    return granted
  }

  fun abandon(context: Context, holder: (Int) -> Unit) {
    val release = synchronized(lock) {
      if (!holders.remove(holder) || holders.isNotEmpty() || !registered) return
      registered = false
      hasFocus = false
      request
    } ?: return
    AudioManagerCompat.abandonAudioFocusRequest(audioManager(context), release)
  }

  private fun focusRequest(): AudioFocusRequestCompat =
    request ?: AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
      .setAudioAttributes(
        AudioAttributesCompat.Builder()
          .setUsage(AudioAttributesCompat.USAGE_MEDIA)
          .setContentType(AudioAttributesCompat.CONTENT_TYPE_MOVIE)
          .build()
      )
      .setWillPauseWhenDucked(false)
      .setOnAudioFocusChangeListener(::dispatch, Handler(Looper.getMainLooper()))
      .build()
      .also { request = it }

  private fun dispatch(change: Int) {
    val current = synchronized(lock) {
      hasFocus = change == AudioManager.AUDIOFOCUS_GAIN
      holders.toList()
    }
    current.forEach { it(change) }
  }

  private fun audioManager(context: Context) =
    context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
}
