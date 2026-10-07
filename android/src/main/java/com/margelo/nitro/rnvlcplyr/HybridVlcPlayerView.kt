package com.margelo.nitro.rnvlcplyr

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.Keep
import com.facebook.proguard.annotations.DoNotStrip
import com.facebook.react.uimanager.ThemedReactContext
import com.margelo.nitro.rnvlcplyr.engine.ResizeModes
import com.margelo.nitro.rnvlcplyr.engine.VideoOutput
import com.margelo.nitro.rnvlcplyr.view.VideoContainer
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

@DoNotStrip
@Keep
class HybridVlcPlayerView(private val context: ThemedReactContext) : HybridVlcPlayerViewSpec(), VideoOutput {
  private val container = VideoContainer(context)
  private var attachedPlayer: HybridVlcPlayer? = null

  override val view: View = container

  override var player: HybridVlcPlayerSpec? = null
  override var resizeMode: VlcResizeMode? = null
  override var surfaceType: VlcSurfaceType? = null

  override var usesTextureView: Boolean = true
    private set

  override var videoLayout: VLCVideoLayout = createVideoLayout()
    private set

  override val scaleType: MediaPlayer.ScaleType
    get() = ResizeModes.scaleTypeFor(resizeMode ?: ResizeModes.DEFAULT)

  override fun afterUpdate() {
    syncSurfaceType()
    syncPlayer()
  }

  override fun onDropView() {
    detachFromPlayer()
  }

  private fun syncSurfaceType() {
    val wantsTexture = (surfaceType ?: DEFAULT_SURFACE_TYPE) == VlcSurfaceType.TEXTURE
    if (wantsTexture == usesTextureView) return
    usesTextureView = wantsTexture
    renewVideoLayout()
    attachedPlayer?.reattachOutput(this)
  }

  override fun renewVideoLayout() {
    container.removeView(videoLayout)
    videoLayout = createVideoLayout()
  }

  private fun syncPlayer() {
    val target = player as? HybridVlcPlayer
    if (target === attachedPlayer) {
      target?.applyScale(this)
      return
    }
    detachFromPlayer()
    attachedPlayer = target
    target?.attachOutput(this)
  }

  private fun detachFromPlayer() {
    attachedPlayer?.detachOutput(this)
    attachedPlayer = null
  }

  private fun createVideoLayout(): VLCVideoLayout =
    VLCVideoLayout(context).also { layout ->
      container.addView(layout, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

  private companion object {
    val DEFAULT_SURFACE_TYPE = VlcSurfaceType.TEXTURE
  }
}
