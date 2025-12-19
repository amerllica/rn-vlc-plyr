package com.rnvlcplyr

import android.graphics.Color
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.viewmanagers.RnVlcPlyrViewManagerInterface
import com.facebook.react.viewmanagers.RnVlcPlyrViewManagerDelegate

@ReactModule(name = RnVlcPlyrViewManager.NAME)
class RnVlcPlyrViewManager : SimpleViewManager<RnVlcPlyrView>(), RnVlcPlyrViewManagerInterface<RnVlcPlyrView> {
  private val mDelegate: ViewManagerDelegate<RnVlcPlyrView>

  init {
    mDelegate = RnVlcPlyrViewManagerDelegate(this)
  }

  override fun getDelegate(): ViewManagerDelegate<RnVlcPlyrView>? {
    return mDelegate
  }

  override fun getName(): String {
    return NAME
  }

  public override fun createViewInstance(context: ThemedReactContext): RnVlcPlyrView {
    return RnVlcPlyrView(context)
  }

  @ReactProp(name = "url")
  override fun setUrl(view: RnVlcPlyrView, url: String?) {
    view.setUrl(url)
  }

  @ReactProp(name = "autoPlay", defaultBoolean = true)
  override fun setAutoPlay(view: RnVlcPlyrView, autoPlay: Boolean) {
      view.setAutoPlay(autoPlay)
  }

  @ReactProp(name = "loop", defaultBoolean = false)
  override fun setLoop(view: RnVlcPlyrView, loop: Boolean) {
      view.setLoop(loop)
  }

  @ReactProp(name = "muted", defaultBoolean = false)
  override fun setMuted(view: RnVlcPlyrView, muted: Boolean) {
      view.setMuted(muted)
  }


  override fun play(view: RnVlcPlyrView) {
    view.play()
  }

  override fun pause(view: RnVlcPlyrView) {
    view.pause()
  }

  override fun stop(view: RnVlcPlyrView) {
    view.stop()
  }

  override fun presentFullscreen(view: RnVlcPlyrView) {
      // empty temporary
  }

  override fun dismissFullscreen(view: RnVlcPlyrView) {
      // empty temporary
  }

  override fun seek(view: RnVlcPlyrView, time: Double) {
      view.seek(time)
  }

  override fun setVolume(view: RnVlcPlyrView, volume: Double) {
      view.setVolume(volume)
  }

  override fun onDropViewInstance(view: RnVlcPlyrView) {
    super.onDropViewInstance(view)
    view.cleanup()
  }

  companion object {
    const val NAME = "RnVlcPlyrView"
  }
}
