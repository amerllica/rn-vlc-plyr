package com.margelo.nitro.rnvlcplyr.session

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.margelo.nitro.rnvlcplyr.R
import com.margelo.nitro.rnvlcplyr.contract.MediaSessionPlayback
import com.margelo.nitro.rnvlcplyr.contract.RemoteCommand

class MediaSessionBridge(context: Context, private val dispatch: RemoteCommandSink) {
  private val session = MediaSessionCompat(context, SESSION_TAG)

  val token: MediaSessionCompat.Token
    get() = session.sessionToken

  val isActive: Boolean
    get() = session.isActive

  init {
    session.setCallback(RemoteCallback(), Handler(Looper.getMainLooper()))
  }

  fun publish(content: SessionContent) {
    session.setMetadata(metadataOf(content))
    session.setPlaybackState(playbackStateOf(content))
    session.isActive = content.state.isActive
  }

  fun clear() {
    session.isActive = false
    session.setPlaybackState(PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_NONE, 0L, 0f).build())
    session.setMetadata(null)
  }

  private fun metadataOf(content: SessionContent): MediaMetadataCompat {
    val builder = MediaMetadataCompat.Builder()
      .putString(MediaMetadataCompat.METADATA_KEY_TITLE, content.nowPlaying.title)
      .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, content.nowPlaying.artist)
      .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, content.nowPlaying.album)
    if (content.state.durationMs > 0) builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, content.state.durationMs)
    content.artwork?.let { builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it) }
    return builder.build()
  }

  private fun playbackStateOf(content: SessionContent): PlaybackStateCompat {
    val state = content.state
    val builder = PlaybackStateCompat.Builder()
      .setState(stateCodeOf(state.playback), state.positionMs, state.playbackRate.toFloat(), SystemClock.elapsedRealtime())
      .setActions(state.commands.fold(0L) { actions, command -> actions or actionOf(command) })
    if (RemoteCommand.SKIP_BACKWARD in state.commands) {
      builder.addCustomAction(SKIP_BACKWARD_ACTION, SKIP_BACKWARD_LABEL, R.drawable.rn_vlc_plyr_skip_backward)
    }
    if (RemoteCommand.SKIP_FORWARD in state.commands) {
      builder.addCustomAction(SKIP_FORWARD_ACTION, SKIP_FORWARD_LABEL, R.drawable.rn_vlc_plyr_skip_forward)
    }
    return builder.build()
  }

  private fun stateCodeOf(playback: MediaSessionPlayback): Int = when (playback) {
    MediaSessionPlayback.NONE -> PlaybackStateCompat.STATE_NONE
    MediaSessionPlayback.BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
    MediaSessionPlayback.PLAYING -> PlaybackStateCompat.STATE_PLAYING
    MediaSessionPlayback.PAUSED -> PlaybackStateCompat.STATE_PAUSED
    MediaSessionPlayback.STOPPED -> PlaybackStateCompat.STATE_STOPPED
    MediaSessionPlayback.ERROR -> PlaybackStateCompat.STATE_ERROR
  }

  private fun actionOf(command: RemoteCommand): Long = when (command) {
    RemoteCommand.PLAY -> PlaybackStateCompat.ACTION_PLAY
    RemoteCommand.PAUSE -> PlaybackStateCompat.ACTION_PAUSE
    RemoteCommand.TOGGLE_PLAY_PAUSE -> PlaybackStateCompat.ACTION_PLAY_PAUSE
    RemoteCommand.STOP -> PlaybackStateCompat.ACTION_STOP
    RemoteCommand.SEEK -> PlaybackStateCompat.ACTION_SEEK_TO
    RemoteCommand.NEXT -> PlaybackStateCompat.ACTION_SKIP_TO_NEXT
    RemoteCommand.PREVIOUS -> PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
    RemoteCommand.SKIP_FORWARD, RemoteCommand.SKIP_BACKWARD -> NO_ACTION
  }

  private inner class RemoteCallback : MediaSessionCompat.Callback() {
    override fun onPlay() = dispatch(RemoteCommand.PLAY, NO_POSITION)

    override fun onPause() = dispatch(RemoteCommand.PAUSE, NO_POSITION)

    override fun onStop() = dispatch(RemoteCommand.STOP, NO_POSITION)

    override fun onSeekTo(pos: Long) = dispatch(RemoteCommand.SEEK, pos)

    override fun onSkipToNext() = dispatch(RemoteCommand.NEXT, NO_POSITION)

    override fun onSkipToPrevious() = dispatch(RemoteCommand.PREVIOUS, NO_POSITION)

    override fun onCustomAction(action: String?, extras: Bundle?) {
      when (action) {
        SKIP_BACKWARD_ACTION -> dispatch(RemoteCommand.SKIP_BACKWARD, NO_POSITION)
        SKIP_FORWARD_ACTION -> dispatch(RemoteCommand.SKIP_FORWARD, NO_POSITION)
      }
    }
  }

  companion object {
    const val SKIP_BACKWARD_ACTION = "rn-vlc-plyr.skipBackward"
    const val SKIP_FORWARD_ACTION = "rn-vlc-plyr.skipForward"
    const val NO_POSITION = 0L
    private const val SESSION_TAG = "RnVlcPlyr"
    private const val SKIP_BACKWARD_LABEL = "Skip back"
    private const val SKIP_FORWARD_LABEL = "Skip forward"
    private const val NO_ACTION = 0L
  }
}
