package com.margelo.nitro.rnvlcplyr.contract

class ListenerRegistry<T>(
  private val onListenerFailure: (Throwable) -> Unit = {},
  private val onCountChange: (previous: Int, current: Int) -> Unit = { _, _ -> }
) {
  private val lock = Any()
  private val listeners = LinkedHashMap<Long, (T) -> Unit>()
  private var nextId = 0L

  val count: Int
    get() = synchronized(lock) { listeners.size }

  fun add(listener: (T) -> Unit): () -> Unit {
    val (id, current) = synchronized(lock) {
      val assigned = nextId++
      listeners[assigned] = listener
      assigned to listeners.size
    }
    onCountChange(current - 1, current)
    return { remove(id) }
  }

  fun emit(value: T) {
    val snapshot = synchronized(lock) { listeners.values.toList() }
    snapshot.forEach { listener ->
      try {
        listener(value)
      } catch (failure: Throwable) {
        onListenerFailure(failure)
      }
    }
  }

  fun clear() {
    val previous = synchronized(lock) {
      val size = listeners.size
      listeners.clear()
      size
    }
    if (previous > 0) onCountChange(previous, 0)
  }

  private fun remove(id: Long) {
    val current = synchronized(lock) { if (listeners.remove(id) == null) null else listeners.size } ?: return
    onCountChange(current + 1, current)
  }
}
