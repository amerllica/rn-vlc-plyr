package com.margelo.nitro.rnvlcplyr.session

import java.lang.ref.WeakReference

object MediaSessionOwner {
  private var owner: WeakReference<MediaSessionCoordinator>? = null

  @Synchronized
  fun current(): MediaSessionCoordinator? = owner?.get()

  @Synchronized
  fun isOwner(coordinator: MediaSessionCoordinator): Boolean = owner?.get() === coordinator

  @Synchronized
  fun claim(coordinator: MediaSessionCoordinator) {
    owner = WeakReference(coordinator)
  }

  @Synchronized
  fun relinquish(coordinator: MediaSessionCoordinator): Boolean {
    if (owner?.get() !== coordinator) return false
    owner = null
    return true
  }
}
