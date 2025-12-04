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
class RnVlcPlyrViewManager : SimpleViewManager<RnVlcPlyrView>(),
  RnVlcPlyrViewManagerInterface<RnVlcPlyrView> {
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

  @ReactProp(name = "color")
  override fun setColor(view: RnVlcPlyrView?, color: String?) {
    view?.setBackgroundColor(Color.parseColor(color))
  }

  companion object {
    const val NAME = "RnVlcPlyrView"
  }
}
