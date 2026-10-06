package com.margelo.nitro.rnvlcplyr

import android.content.Context
import com.margelo.nitro.rnvlcplyr.engine.FrameCapture
import com.margelo.nitro.rnvlcplyr.engine.SnapshotWriter
import com.margelo.nitro.rnvlcplyr.engine.VideoOutput

object SnapshotCapture {
  const val NO_FRAME_MESSAGE = "No video frame available"

  fun capture(context: Context, output: VideoOutput, size: VlcVideoSize, onResult: (Result<String>) -> Unit) {
    FrameCapture.capture(output.videoLayout, size.width.toInt(), size.height.toInt()) { bitmap ->
      if (bitmap == null) {
        onResult(Result.failure(VlcPlayerError(NO_FRAME_MESSAGE)))
      } else {
        SnapshotWriter.writePng(context, bitmap, onResult)
      }
    }
  }
}
