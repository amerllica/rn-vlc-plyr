package com.margelo.nitro.rnvlcplyr.session

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.margelo.nitro.rnvlcplyr.engine.MainThread

class AppLifecycleObserver(
  private val onBackground: () -> Unit,
  private val onForeground: () -> Unit
) : DefaultLifecycleObserver {
  override fun onStart(owner: LifecycleOwner) = onForeground()

  override fun onStop(owner: LifecycleOwner) = onBackground()

  fun attach() = MainThread.run { ProcessLifecycleOwner.get().lifecycle.addObserver(this) }

  fun detach() = MainThread.run { ProcessLifecycleOwner.get().lifecycle.removeObserver(this) }
}
