package com.margelo.nitro.rnvlcplyr.session

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.margelo.nitro.rnvlcplyr.R
import com.margelo.nitro.rnvlcplyr.VlcAndroidNotification
import com.margelo.nitro.rnvlcplyr.contract.RemoteCommand

class PlaybackNotification(private val context: Context, private val dispatch: RemoteCommandSink) {
  private class LegacyAction(val command: RemoteCommand, val action: NotificationCompat.Action)

  private val manager = NotificationManagerCompat.from(context)
  private val contentIntent: PendingIntent? by lazy {
    context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
      PendingIntent.getActivity(context, CONTENT_REQUEST_CODE, it, PENDING_INTENT_FLAGS)
    }
  }
  private val actionReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val name = intent.getStringExtra(EXTRA_COMMAND)
      RemoteCommand.entries.firstOrNull { it.name == name }?.let { dispatch(it, MediaSessionBridge.NO_POSITION) }
    }
  }
  private var actionReceiverRegistered = false

  fun build(content: SessionContent, token: MediaSessionCompat.Token): Notification {
    val actions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) legacyActions(content) else emptyList()
    val style = MediaStyle().setMediaSession(token)
    if (actions.isNotEmpty()) style.setShowActionsInCompactView(*compactIndices(actions))
    val builder = baseBuilder(context, content.notificationStyle)
      .setContentTitle(content.nowPlaying.title)
      .setContentText(content.nowPlaying.artist)
      .setSubText(content.nowPlaying.album)
      .setLargeIcon(content.artwork)
      .setContentIntent(contentIntent)
      .setOngoing(content.isPlaying)
      .setStyle(style)
    actions.forEach { builder.addAction(it.action) }
    return builder.build()
  }

  @SuppressLint("MissingPermission")
  fun post(notification: Notification) {
    try {
      manager.notify(NOTIFICATION_ID, notification)
    } catch (denied: SecurityException) {
      Log.w(TAG, "rn-vlc-plyr: the playback notification could not be posted", denied)
    }
  }

  fun cancel() {
    manager.cancel(NOTIFICATION_ID)
  }

  private fun legacyActions(content: SessionContent): List<LegacyAction> {
    registerActionReceiver()
    val commands = content.state.commands
    return listOfNotNull(
      if (RemoteCommand.PREVIOUS in commands) legacyAction(RemoteCommand.PREVIOUS, R.drawable.rn_vlc_plyr_previous, "Previous") else null,
      if (RemoteCommand.SKIP_BACKWARD in commands) legacyAction(RemoteCommand.SKIP_BACKWARD, R.drawable.rn_vlc_plyr_skip_backward, "Skip back") else null,
      if (content.isPlaying) legacyAction(RemoteCommand.TOGGLE_PLAY_PAUSE, R.drawable.rn_vlc_plyr_pause, "Pause")
      else legacyAction(RemoteCommand.TOGGLE_PLAY_PAUSE, R.drawable.rn_vlc_plyr_play, "Play"),
      if (RemoteCommand.SKIP_FORWARD in commands) legacyAction(RemoteCommand.SKIP_FORWARD, R.drawable.rn_vlc_plyr_skip_forward, "Skip forward") else null,
      if (RemoteCommand.NEXT in commands) legacyAction(RemoteCommand.NEXT, R.drawable.rn_vlc_plyr_next, "Next") else null
    )
  }

  private fun legacyAction(command: RemoteCommand, icon: Int, title: String): LegacyAction {
    val intent = Intent(ACTION_REMOTE_COMMAND).setPackage(context.packageName).putExtra(EXTRA_COMMAND, command.name)
    val pendingIntent = PendingIntent.getBroadcast(context, command.ordinal, intent, PENDING_INTENT_FLAGS)
    return LegacyAction(command, NotificationCompat.Action(icon, title, pendingIntent))
  }

  private fun compactIndices(actions: List<LegacyAction>): IntArray {
    val toggleIndex = actions.indexOfFirst { it.command == RemoteCommand.TOGGLE_PLAY_PAUSE }
    return ((toggleIndex - 1)..(toggleIndex + 1)).filter { it in actions.indices }.toIntArray()
  }

  private fun registerActionReceiver() {
    if (actionReceiverRegistered) return
    actionReceiverRegistered = true
    ContextCompat.registerReceiver(context, actionReceiver, IntentFilter(ACTION_REMOTE_COMMAND), ContextCompat.RECEIVER_NOT_EXPORTED)
  }

  companion object {
    const val NOTIFICATION_ID = 0x564C43
    const val CHANNEL_ID = "rn_vlc_plyr_playback"
    private const val CHANNEL_NAME = "Playback"
    private const val TAG = "RnVlcPlyr"
    private const val ACTION_REMOTE_COMMAND = "com.margelo.nitro.rnvlcplyr.REMOTE_COMMAND"
    private const val EXTRA_COMMAND = "command"
    private const val CONTENT_REQUEST_CODE = 0
    private const val PENDING_INTENT_FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    @Volatile
    private var channelCreated = false

    fun placeholder(context: Context): Notification = baseBuilder(context, null).build()

    private fun baseBuilder(context: Context, style: VlcAndroidNotification?): NotificationCompat.Builder {
      ensureChannel(context)
      val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(smallIconOf(context, style))
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setOnlyAlertOnce(true)
        .setShowWhen(false)
        .setSilent(true)
      colorOf(style)?.let(builder::setColor)
      return builder
    }

    private fun ensureChannel(context: Context) {
      if (channelCreated) return
      val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
        .setName(CHANNEL_NAME)
        .setShowBadge(false)
        .build()
      NotificationManagerCompat.from(context).createNotificationChannel(channel)
      channelCreated = true
    }

    @SuppressLint("DiscouragedApi")
    private fun smallIconOf(context: Context, style: VlcAndroidNotification?): Int {
      val named = style?.smallIcon?.let { context.resources.getIdentifier(it, "drawable", context.packageName) } ?: 0
      return named.takeIf { it != 0 }
        ?: context.applicationInfo.icon.takeIf { it != 0 }
        ?: R.drawable.rn_vlc_plyr_play
    }

    private fun colorOf(style: VlcAndroidNotification?): Int? =
      style?.color?.let { runCatching { Color.parseColor(it) }.getOrNull() }
  }
}
