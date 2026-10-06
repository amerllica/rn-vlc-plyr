package com.margelo.nitro.rnvlcplyr.contract

class AttachmentStack<T : Any> {
  private val items = mutableListOf<T>()

  val top: T?
    get() = items.lastOrNull()

  val size: Int
    get() = items.size

  fun attach(item: T): Boolean {
    val previousTop = top
    items.remove(item)
    items.add(item)
    return previousTop !== item
  }

  fun detach(item: T): Boolean {
    val previousTop = top
    items.remove(item)
    return previousTop !== top
  }

  fun isTop(item: T): Boolean = top === item

  fun clear() {
    items.clear()
  }
}
