package com.margelo.nitro.rnvlcplyr.contract

class ChangeFilter<T>(private val initial: T) {
  private var lastEmitted: T = initial

  fun offer(value: T): Boolean {
    if (value == lastEmitted) return false
    lastEmitted = value
    return true
  }

  fun reset() {
    lastEmitted = initial
  }
}
