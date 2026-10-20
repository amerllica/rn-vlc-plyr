package com.margelo.nitro.rnvlcplyr

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import com.margelo.nitro.rnvlcplyr.engine.VideoOutput
import com.margelo.nitro.rnvlcplyr.view.VideoContainer
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class VideoHostActivity : Activity() {
  lateinit var container: VideoContainer
    private set

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    container = VideoContainer(this)
    setContentView(ReactLikeHost(this).apply { addView(container) })
  }

  fun createOutput(usesTextureView: Boolean): HostVideoOutput = HostVideoOutput(container, usesTextureView)
}

private class ReactLikeHost(context: Context) : ViewGroup(context) {
  override fun requestLayout() {
    if (!isLaidOut) super.requestLayout()
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    for (index in 0 until childCount) {
      val child = getChildAt(index)
      child.measure(
        MeasureSpec.makeMeasureSpec(right - left, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(bottom - top, MeasureSpec.EXACTLY)
      )
      child.layout(0, 0, right - left, bottom - top)
    }
  }
}

class HostVideoOutput(
  private val container: VideoContainer,
  usesTextureView: Boolean
) : VideoOutput {
  override var usesTextureView: Boolean = usesTextureView
    private set

  override var videoLayout: VLCVideoLayout = createVideoLayout()
    private set

  override val scaleType: MediaPlayer.ScaleType = MediaPlayer.ScaleType.SURFACE_BEST_FIT

  fun useSurfaceType(usesTextureView: Boolean) {
    this.usesTextureView = usesTextureView
  }

  override fun renewVideoLayout() {
    container.removeView(videoLayout)
    videoLayout = createVideoLayout()
  }

  private fun createVideoLayout(): VLCVideoLayout =
    VLCVideoLayout(container.context).also { layout ->
      container.addView(layout, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }
}
