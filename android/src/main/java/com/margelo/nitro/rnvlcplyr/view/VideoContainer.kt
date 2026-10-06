package com.margelo.nitro.rnvlcplyr.view

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout

class VideoContainer(context: Context) : FrameLayout(context) {
  private val relayout = Runnable { measureAndLayoutChildren() }

  init {
    setBackgroundColor(Color.BLACK)
    clipChildren = true
  }

  override fun requestLayout() {
    super.requestLayout()
    post(relayout)
  }

  private fun measureAndLayoutChildren() {
    measure(
      View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
    )
    layout(left, top, right, bottom)
  }
}
