package com.margelo.nitro.rnvlcplyr.contract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentStackTest {
  @Test
  fun detachRestoresThePreviousTop() {
    val stack = AttachmentStack<String>()
    assertTrue(stack.attach("a"))
    assertTrue(stack.attach("b"))
    assertFalse(stack.attach("b"))
    assertFalse(stack.detach("a"))
    assertTrue(stack.attach("a"))
    assertTrue(stack.detach("a"))
    assertEquals("b", stack.top)
    assertTrue(stack.detach("b"))
    assertNull(stack.top)
  }

  @Test
  fun clearReturnsEveryAttachedItemAndEmptiesTheStack() {
    val stack = AttachmentStack<String>()
    stack.attach("a")
    stack.attach("b")
    assertEquals(listOf("a", "b"), stack.clear())
    assertNull(stack.top)
    assertEquals(emptyList<String>(), stack.clear())
  }
}
