package com.margelo.nitro.rnvlcplyr.session

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import com.margelo.nitro.rnvlcplyr.VlcStatus
import com.margelo.nitro.rnvlcplyr.contract.BackgroundEffect
import com.margelo.nitro.rnvlcplyr.contract.BackgroundPolicy
import com.margelo.nitro.rnvlcplyr.contract.InterruptionEffect
import com.margelo.nitro.rnvlcplyr.contract.InterruptionPolicy
import com.margelo.nitro.rnvlcplyr.contract.RemoteCommand
import com.margelo.nitro.rnvlcplyr.contract.isActive
import com.margelo.nitro.rnvlcplyr.engine.MainThread

class MediaSessionCoordinator(context: Context, private val host: SessionHost) {
  private val context = context.applicationContext
  private val background = BackgroundPolicy()
  private val interruption = InterruptionPolicy()
  private val focus = AudioFocusController(this.context, ::onFocusChange, ::onBecomingNoisy)
  private val lifecycle = AppLifecycleObserver(::onEnterBackground, ::onEnterForeground)
  private var artworkUri: String? = null
  private var artwork: Bitmap? = null

  @Volatile
  private var released = false

  init {
    lifecycle.attach()
  }

  fun refresh() = onMain { publish() }

  fun onStatusChanged() = onMain {
    focus.listenForNoisy(host.sessionSnapshot().status.isActive)
    publish()
  }

  fun onSourceChanged(cleared: Boolean) = onMain {
    interruption.reset()
    if (cleared) focus.abandon()
    publish()
  }

  fun onUserCommand() = onMain { interruption.reset() }

  fun onStopped() = onMain {
    interruption.reset()
    focus.abandon()
  }

  fun requestFocus() {
    if (!released) focus.request()
  }

  fun staysActiveChanged(staysActive: Boolean) = onMain {
    if (staysActive) PlaybackServiceSetup.warnIfMissing(context)
    applyBackground(background.staysActiveChanged(staysActive, isPlayerActive()))
    publish()
  }

  fun handleRemoteCommand(command: RemoteCommand, positionMs: Long) = onMain {
    when (command) {
      RemoteCommand.PLAY -> host.play()
      RemoteCommand.PAUSE -> host.pause()
      RemoteCommand.TOGGLE_PLAY_PAUSE -> togglePlayPause()
      RemoteCommand.STOP -> host.stop()
      RemoteCommand.SEEK -> host.seek(positionMs.toDouble())
      RemoteCommand.SKIP_FORWARD -> host.seekBy(skipIntervalMs())
      RemoteCommand.SKIP_BACKWARD -> host.seekBy(-skipIntervalMs())
      RemoteCommand.NEXT -> host.events.remoteNext.emit(Unit)
      RemoteCommand.PREVIOUS -> host.events.remotePrevious.emit(Unit)
    }
  }

  fun stopForTaskRemoval() = onMain {
    host.stop()
    giveUp()
  }

  fun release() = MainThread.run {
    if (released) return@run
    released = true
    lifecycle.detach()
    focus.release()
    giveUp()
  }

  private fun publish() {
    val snapshot = host.sessionSnapshot()
    val state = snapshot.sessionState
    val nowPlaying = snapshot.nowPlaying
    if (!state.isActive || nowPlaying == null) return giveUp()
    if (!MediaSessionOwner.isOwner(this)) {
      if (snapshot.status != VlcStatus.PLAYING) return
      MediaSessionOwner.claim(this)
    }
    val content = SessionContent(
      state = state,
      nowPlaying = nowPlaying,
      artwork = artworkFor(nowPlaying.artworkUri),
      notificationStyle = snapshot.androidNotification,
      staysActiveInBackground = snapshot.staysActiveInBackground
    )
    SystemMediaSession.publish(context, content)
  }

  private fun giveUp() {
    if (MediaSessionOwner.relinquish(this)) SystemMediaSession.clear(context)
  }

  private fun artworkFor(uri: String?): Bitmap? {
    if (uri == artworkUri) return artwork
    artworkUri = uri
    artwork = uri?.let(ArtworkLoader::cached)
    if (uri != null && artwork == null) ArtworkLoader.load(context, uri) { loaded -> onMain { applyArtwork(uri, loaded) } }
    return artwork
  }

  private fun applyArtwork(uri: String, loaded: Bitmap?) {
    if (loaded == null || uri != artworkUri) return
    artwork = loaded
    if (MediaSessionOwner.isOwner(this)) publish()
  }

  private fun togglePlayPause() {
    val status = host.sessionSnapshot().status
    if (status == VlcStatus.PLAYING || status == VlcStatus.BUFFERING) host.pause() else host.play()
  }

  private fun onFocusChange(change: Int) = onMain {
    val effect = when (change) {
      AudioManager.AUDIOFOCUS_LOSS -> interruption.lostPermanently(isPlayerActive())
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> interruption.began(isPlayerActive())
      AudioManager.AUDIOFOCUS_GAIN -> interruption.ended(shouldResume = true)
      else -> null
    }
    applyInterruption(effect)
  }

  private fun onBecomingNoisy() = onMain { applyInterruption(interruption.becameNoisy(isPlayerActive())) }

  private fun onEnterBackground() = onMain {
    val snapshot = host.sessionSnapshot()
    applyBackground(background.enterBackground(snapshot.staysActiveInBackground, snapshot.status.isActive))
  }

  private fun onEnterForeground() = onMain { applyBackground(background.enterForeground()) }

  private fun applyInterruption(effect: InterruptionEffect?) {
    when (effect) {
      InterruptionEffect.PAUSE -> host.pauseForSession()
      InterruptionEffect.RESUME -> host.play()
      null -> Unit
    }
  }

  private fun applyBackground(effects: List<BackgroundEffect>) {
    effects.forEach { effect ->
      when (effect) {
        BackgroundEffect.PAUSE -> {
          interruption.reset()
          host.pauseForSession()
        }
        BackgroundEffect.DISABLE_VIDEO -> host.setVideoTrackEnabled(false)
        BackgroundEffect.ENABLE_VIDEO -> host.setVideoTrackEnabled(true)
      }
    }
  }

  private fun isPlayerActive(): Boolean = host.sessionSnapshot().status.isActive

  private fun skipIntervalMs(): Double = host.sessionSnapshot().remoteSkipIntervalMs.toDouble()

  private fun onMain(block: () -> Unit) = MainThread.run { if (!released) block() }
}
