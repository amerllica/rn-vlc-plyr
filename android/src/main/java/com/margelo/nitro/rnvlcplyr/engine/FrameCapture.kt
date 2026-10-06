package com.margelo.nitro.rnvlcplyr.engine

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import org.videolan.R
import org.videolan.libvlc.util.VLCVideoLayout

object FrameCapture {
  fun capture(layout: VLCVideoLayout, width: Int, height: Int, onResult: (Bitmap?) -> Unit) {
    val texture = layout.findViewById<TextureView>(R.id.texture_video)
    if (texture != null) {
      onResult(captureTexture(texture, width, height))
      return
    }
    val surface = layout.findViewById<SurfaceView>(R.id.surface_video)
    if (surface == null || !surface.holder.surface.isValid) {
      onResult(null)
      return
    }
    captureSurface(surface, width, height, onResult)
  }

  private fun captureTexture(texture: TextureView, width: Int, height: Int): Bitmap? =
    if (texture.isAvailable) texture.getBitmap(width, height) else null

  private fun captureSurface(surface: SurfaceView, width: Int, height: Int, onResult: (Bitmap?) -> Unit) {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val onCopied = PixelCopy.OnPixelCopyFinishedListener { result ->
      if (result == PixelCopy.SUCCESS) onResult(bitmap) else onResult(null)
    }
    try {
      PixelCopy.request(surface, bitmap, onCopied, Handler(Looper.getMainLooper()))
    } catch (failure: IllegalArgumentException) {
      onResult(null)
    }
  }
}
