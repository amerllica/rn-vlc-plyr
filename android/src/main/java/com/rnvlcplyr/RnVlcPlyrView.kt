package com.rnvlcplyr

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.util.ArrayList
import android.net.Uri

class RnVlcPlyrView : FrameLayout {

    private var libVlc: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var videoLayout: VLCVideoLayout? = null

    private var lastVolumeBeforeMute: Int = 100
    private var isEnded = false

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

        mediaPlayer?.setEventListener { event ->
            if (event.type == MediaPlayer.Event.EndReached) {
                isEnded = true
            }
        }
    }

    fun setUrl(newUrl: String?) {
        if (newUrl.isNullOrEmpty()) return
        if (url == newUrl) return
        url = newUrl

        isEnded = false

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
            return
        }

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
        mediaPlayer?.let {
            it.stop()
        }
    }

    fun seek(timeMs: Double) {
        if (timeMs < 0) return

        mediaPlayer?.let {
            it.time = timeMs.toLong()
        }
    }

    fun setVolume(volume: Double) {
        mediaPlayer?.let {
            val vol = volume.toInt().coerceIn(0, 100)
            it?.volume = vol

            if (vol > 0) {
                lastVolumeBeforeMute = vol
            }
        }
    }

    fun cleanup() {
        mediaPlayer?.detachViews()
        mediaPlayer?.release()
        libVlc?.release()
        mediaPlayer = null
        libVlc = null
    }
}
