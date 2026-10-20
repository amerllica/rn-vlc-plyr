package com.margelo.nitro.rnvlcplyr.session

import android.graphics.Bitmap
import com.margelo.nitro.rnvlcplyr.VlcAndroidNotification
import com.margelo.nitro.rnvlcplyr.VlcNowPlaying
import com.margelo.nitro.rnvlcplyr.contract.MediaSessionPlayback
import com.margelo.nitro.rnvlcplyr.contract.MediaSessionState
import com.margelo.nitro.rnvlcplyr.contract.RemoteCommand

typealias RemoteCommandSink = (command: RemoteCommand, positionMs: Long) -> Unit

data class SessionContent(
  val state: MediaSessionState,
  val nowPlaying: VlcNowPlaying,
  val artwork: Bitmap?,
  val notificationStyle: VlcAndroidNotification?,
  val staysActiveInBackground: Boolean
) {
  val isPlaying: Boolean
    get() = state.playback == MediaSessionPlayback.PLAYING || state.playback == MediaSessionPlayback.BUFFERING
}
