package com.rnvlcplyr

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.util.ArrayList

class RnVlcPlyrView : FrameLayout {

    private var libVlc: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var videoLayout: VLCVideoLayout? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    private fun init(context: Context) {
        val args = ArrayList<String>()
        args.add("-vvv")
        libVlc = LibVLC(context, args)
        mediaPlayer = MediaPlayer(libVlc)

        videoLayout = VLCVideoLayout(context)
        addView(videoLayout)

        mediaPlayer?.attachViews(videoLayout!!, null, false, false)
    }

    fun setUrl(url: String?) {
        if (url.isNullOrEmpty()) return

        try {
            val media = Media(libVlc, android.net.Uri.parse(url))
            mediaPlayer?.media = media
            media.release()
            mediaPlayer?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun play() {
        if (mediaPlayer?.isPlaying == false) {
            mediaPlayer?.play()
        }
    }

    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    fun stop() {
        mediaPlayer?.stop()
    }

    fun cleanup() {
        mediaPlayer?.release()
        libVlc?.release()
    }
}
