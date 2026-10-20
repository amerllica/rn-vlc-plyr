package com.margelo.nitro.rnvlcplyr.session

import android.app.Notification
import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import com.margelo.nitro.rnvlcplyr.VlcPlaybackService
import com.margelo.nitro.rnvlcplyr.contract.MediaSessionPlayback
import com.margelo.nitro.rnvlcplyr.contract.RemoteCommand

object SystemMediaSession {
  private var bridge: MediaSessionBridge? = null
  private var notifications: PlaybackNotification? = null
  private var foreground = false

  var currentNotification: Notification? = null
    private set

  val sessionToken: MediaSessionCompat.Token?
    get() = bridge?.token

  val isSessionActive: Boolean
    get() = bridge?.isActive == true

  fun publish(context: Context, content: SessionContent) {
    val appContext = context.applicationContext
    val activeBridge = bridge ?: MediaSessionBridge(appContext, ::dispatch).also { bridge = it }
    val notificationBuilder = notifications ?: PlaybackNotification(appContext, ::dispatch).also { notifications = it }
    activeBridge.publish(content)
    val notification = notificationBuilder.build(content, activeBridge.token)
    currentNotification = notification
    notificationBuilder.post(notification)
    updateService(appContext, content)
  }

  fun clear(context: Context) {
    val appContext = context.applicationContext
    if (PlaybackServiceSetup.isDeclared(appContext)) VlcPlaybackService.stop(appContext)
    foreground = false
    currentNotification = null
    notifications?.cancel()
    bridge?.clear()
  }

  fun onServiceStopped() {
    foreground = false
  }

  private fun updateService(context: Context, content: SessionContent) {
    val playback = content.state.playback
    val keepsPlaying = playback == MediaSessionPlayback.PLAYING || (foreground && playback == MediaSessionPlayback.BUFFERING)
    val wantsForeground = keepsPlaying && content.staysActiveInBackground && PlaybackServiceSetup.isDeclared(context)
    if (wantsForeground && !foreground) {
      foreground = VlcPlaybackService.start(context)
    } else if (!wantsForeground && foreground) {
      VlcPlaybackService.demote()
      foreground = false
    }
  }

  private fun dispatch(command: RemoteCommand, positionMs: Long) {
    MediaSessionOwner.current()?.handleRemoteCommand(command, positionMs)
  }
}
