package com.rnvlcplyr

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.uimanager.common.ViewUtil
import com.facebook.react.uimanager.events.Event
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import android.net.Uri

class RnVlcPlyrView : FrameLayout {

  private var libVlc: LibVLC? = null
  private var mediaPlayer: MediaPlayer? = null
  private var videoLayout: VLCVideoLayout? = null

  private var lastVolumeBeforeMute = 100
  private var isEnded = false
  private var hasReportedError = false

  private var url: String? = null
  private var autoPlay: Boolean = true
  private var loop: Boolean = false
  private var muted: Boolean = false

  constructor(context: Context) : super(context) {
    init(context)
  }

  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    init(context)
  }

  private fun init(context: Context) {
    val args = arrayListOf("-vvv")
    libVlc = LibVLC(context, args)
    mediaPlayer = MediaPlayer(libVlc)

    videoLayout = VLCVideoLayout(context)
    addView(
      videoLayout,
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    )

    mediaPlayer?.attachViews(videoLayout!!, null, false, false)

    setupEventListener()
  }

  private fun setupEventListener() {
    mediaPlayer?.setEventListener { event ->
      when (event.type) {
        MediaPlayer.Event.EndReached -> {
          isEnded = true
        }

        MediaPlayer.Event.EncounteredError -> {
          emitError("VLC Player encountered an error", -1)
        }

        MediaPlayer.Event.Opening ->
          Log.d(TAG, "Media opening")

        MediaPlayer.Event.Playing ->
          Log.d(TAG, "Media playing")

        MediaPlayer.Event.Stopped ->
          Log.d(TAG, "Media stopped")
      }
    }
  }

  private fun emitError(message: String, code: Int) {
    if (hasReportedError) return
    hasReportedError = true

    val reactContext = context as? ReactContext ?: return
    val eventDispatcher = UIManagerHelper.getEventDispatcherForReactTag(reactContext, id) ?: return
    eventDispatcher.dispatchEvent(
      OnErrorEvent(
        UIManagerHelper.getSurfaceId(this),
        id,
        message,
        code
      )
    )
  }

  fun setUrl(newUrl: String?) {
    if (newUrl.isNullOrEmpty()) {
      emitError("Invalid or missing URL", -4)
      return
    }

    if (url == newUrl) return
    url = newUrl

    isEnded = false
    hasReportedError = false

    prepareMedia()
  }

  fun setAutoPlay(_autoPlay: Boolean) {
    if (autoPlay == _autoPlay) return
    autoPlay = _autoPlay

    mediaPlayer?.let { player ->
      if (_autoPlay) {
        if (!player.isPlaying && player.media != null) {
          player.play()
        }
      } else {
        if (player.isPlaying) {
          player.pause()
        }
      }
    }
  }

  fun setLoop(_loop: Boolean) {
    if (loop == _loop) return
    loop = _loop

    val currentTime = mediaPlayer?.time ?: 0L
    prepareMedia(startTime = currentTime)
  }

  fun setMuted(_muted: Boolean) {
    val _mediaPlayer = mediaPlayer ?: return
    if (muted == _muted) return
    muted = _muted

    if (muted) {
      if (_mediaPlayer.volume > 0) {
          lastVolumeBeforeMute = _mediaPlayer.volume
      }
      _mediaPlayer.volume = 0
    } else {
      _mediaPlayer.volume = lastVolumeBeforeMute.coerceIn(0, 100)
    }
  }

  private fun prepareMedia(startTime: Long? = null) {
    val source = url ?: run {
      mediaPlayer?.stop()
      emitError("Invalid or missing URL", -4)
      return
    }

    try {
      val wasPlaying = mediaPlayer?.isPlaying ?: false
      val media = Media(libVlc, Uri.parse(source))

      if (loop) {
          media.addOption(":input-repeat=65535")
      } else {
          media.addOption(":input-repeat=0")
      }

      mediaPlayer?.media = media

      setMuted(muted)

      if (autoPlay || wasPlaying) {
        mediaPlayer?.play()
      }

      startTime?.takeIf { it > 0 }?.let { time ->
          postDelayed({
              mediaPlayer?.time = time
          }, 100)
      }
    } catch (e: Exception) {
      emitError("Failed to prepare media: ${e.message}", -5)
    }
  }

  fun play() {
    val _mediaPlayer = mediaPlayer ?: return

    if (isEnded) {
      _mediaPlayer.stop()
      _mediaPlayer.time = 0
      isEnded = false
    }

    if (!_mediaPlayer.isPlaying) {
      _mediaPlayer.play()
    }
  }

  fun pause() {
    mediaPlayer?.let {
      if (it.isPlaying) it.pause()
    }
  }

  fun stop() {
    mediaPlayer?.stop()
  }

  fun seek(timeMs: Double) {
    if (timeMs < 0 || !timeMs.isFinite()) return

    mediaPlayer?.time = timeMs.toLong()
  }

  fun setVolume(volume: Double) {
    if (!volume.isFinite()) return

    mediaPlayer?.let {
      val vol = volume.toInt().coerceIn(0, 100)
      it.volume = vol

      if (vol > 0) {
        lastVolumeBeforeMute = vol
      }
    }
  }

  fun cleanup() {
    removeCallbacks(null)
    mediaPlayer?.setEventListener(null)
    mediaPlayer?.detachViews()
    mediaPlayer?.release()
    libVlc?.release()
    mediaPlayer = null
    libVlc = null
    hasReportedError = false
  }

  companion object {
    private const val TAG = "RnVlcPlyrView"
  }
}

internal class OnErrorEvent(
  surfaceId: Int,
  viewId: Int,
  private val message: String,
  private val code: Int
) : Event<OnErrorEvent>(surfaceId, viewId) {

  @Deprecated(
    "Use the constructor with surfaceId, viewId, message, and code parameters.",
    replaceWith = ReplaceWith("OnErrorEvent(surfaceId, viewId, message, code)")
  )
  constructor(viewId: Int, message: String, code: Int) : this(ViewUtil.NO_SURFACE_ID, viewId, message, code)

  override fun getEventName(): String = EVENT_NAME

  override fun getEventData(): WritableMap =
    Arguments.createMap().apply {
      putInt("target", viewTag)
      putString("message", message)
      putInt("code", code)
    }

  private companion object {
    private const val EVENT_NAME = "topError"
  }
}
