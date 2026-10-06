package com.margelo.nitro.rnvlcplyr.contract

class ListenerRegistry<T>(private val onListenerFailure: (Throwable) -> Unit = {}) {
  private val lock = Any()
  private val listeners = LinkedHashMap<Long, (T) -> Unit>()
  private var nextId = 0L

  val count: Int
    get() = synchronized(lock) { listeners.size }

  fun add(listener: (T) -> Unit): () -> Unit {
    val id = synchronized(lock) {
      val assigned = nextId++
      listeners[assigned] = listener
      assigned
    }
    return { synchronized(lock) { listeners.remove(id) } }
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
    synchronized(lock) { listeners.clear() }
  }
}
