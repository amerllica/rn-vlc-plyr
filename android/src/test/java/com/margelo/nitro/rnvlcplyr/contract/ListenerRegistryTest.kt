package com.margelo.nitro.rnvlcplyr.contract

import org.junit.Assert.assertEquals
import org.junit.Test

class ListenerRegistryTest {
  @Test
  fun removalIsIdempotentAndFailuresAreIsolated() {
    val failures = mutableListOf<Throwable>()
    val registry = ListenerRegistry<Int>(onListenerFailure = { failures.add(it) })
    val received = mutableListOf<Int>()
    registry.add { throw IllegalStateException("boom") }
    val remove = registry.add { received.add(it) }
    registry.emit(1)
    remove()
    remove()
    registry.emit(2)
    assertEquals(listOf(1), received)
    assertEquals(2, failures.size)
    assertEquals(1, registry.count)
    registry.clear()
    assertEquals(0, registry.count)
  }

  @Test
  fun aListenerMayRemoveItselfWhileEmitting() {
    val registry = ListenerRegistry<Int>()
    var calls = 0
    var remove: () -> Unit = {}
    remove = registry.add {
      calls += 1
      remove()
    }
    registry.emit(1)
    registry.emit(2)
    assertEquals(1, calls)
  }

  @Test
  fun reportsEveryCountChangeOnce() {
    val changes = mutableListOf<Pair<Int, Int>>()
    val registry = ListenerRegistry<Unit>(onCountChange = { previous, current -> changes.add(previous to current) })
    val removeFirst = registry.add {}
    registry.add {}
    removeFirst()
    removeFirst()
    registry.clear()
    registry.clear()
    assertEquals(listOf(0 to 1, 1 to 2, 2 to 1, 1 to 0), changes)
  }
}
