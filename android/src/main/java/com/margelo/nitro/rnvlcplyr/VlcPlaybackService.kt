package com.margelo.nitro.rnvlcplyr

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.margelo.nitro.rnvlcplyr.session.MediaSessionOwner
import com.margelo.nitro.rnvlcplyr.session.PlaybackNotification
import com.margelo.nitro.rnvlcplyr.session.SystemMediaSession

class VlcPlaybackService : Service() {
  override fun onCreate() {
    super.onCreate()
    running = this
  }

  override fun onDestroy() {
    if (running === this) running = null
    SystemMediaSession.onServiceStopped()
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val notification = SystemMediaSession.currentNotification
    val promoted = promote(notification ?: PlaybackNotification.placeholder(this))
    if (!promoted || notification == null) {
      ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
      stopSelf()
    }
    return START_NOT_STICKY
  }

  override fun onTaskRemoved(rootIntent: Intent?) {
    MediaSessionOwner.current()?.stopForTaskRemoval()
    stopSelf()
    super.onTaskRemoved(rootIntent)
  }

  private fun promote(notification: Notification): Boolean = try {
    ServiceCompat.startForeground(this, PlaybackNotification.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
    true
  } catch (failure: RuntimeException) {
    Log.w(TAG, "rn-vlc-plyr: the playback service could not enter the foreground", failure)
    false
  }

  companion object {
    private const val TAG = "RnVlcPlyr"

    @Volatile
    private var running: VlcPlaybackService? = null

    fun start(context: Context): Boolean = try {
      ContextCompat.startForegroundService(context, Intent(context, VlcPlaybackService::class.java))
      true
    } catch (notAllowed: IllegalStateException) {
      Log.w(TAG, "rn-vlc-plyr: the playback service could not start", notAllowed)
      false
    }

    fun demote() {
      running?.let { ServiceCompat.stopForeground(it, ServiceCompat.STOP_FOREGROUND_DETACH) }
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, VlcPlaybackService::class.java))
    }
  }
}
