package com.margelo.nitro.rnvlcplyr.contract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkRedirectTest {
  @Test
  fun redirectStatusesAreRecognised() {
    listOf(301, 302, 303, 307, 308).forEach { assertTrue(ArtworkRedirect.isRedirect(it)) }
    listOf(200, 304, 404, 500).forEach { assertFalse(ArtworkRedirect.isRedirect(it)) }
  }

  @Test
  fun aRedirectFromHttpToHttpsIsFollowed() {
    assertEquals("https://host/cover.png", ArtworkRedirect.target("http://host/cover.png", "https://host/cover.png"))
  }

  @Test
  fun aRelativeLocationIsResolvedAgainstTheRequest() {
    assertEquals("https://host/art/large.png", ArtworkRedirect.target("https://host/art/cover.png", "large.png"))
    assertEquals("https://host/large.png", ArtworkRedirect.target("https://host/art/cover.png", "/large.png"))
  }

  @Test
  fun aRedirectOutOfHttpIsRefused() {
    assertNull(ArtworkRedirect.target("https://host/cover.png", "file:///data/secret.png"))
    assertNull(ArtworkRedirect.target("https://host/cover.png", "content://media/1"))
  }

  @Test
  fun aRedirectFromHttpsBackToHttpIsRefused() {
    assertNull(ArtworkRedirect.target("https://host/cover.png", "http://host/cover.png"))
    assertEquals("http://other/cover.png", ArtworkRedirect.target("http://host/cover.png", "http://other/cover.png"))
  }

  @Test
  fun aLocationWithASpaceIsFollowed() {
    assertEquals("https://host/my cover.png", ArtworkRedirect.target("https://host/a.png", "/my cover.png"))
  }

  @Test
  fun aMissingOrBrokenLocationIsRefused() {
    assertNull(ArtworkRedirect.target("https://host/cover.png", null))
    assertNull(ArtworkRedirect.target("not a url", "/cover.png"))
  }
}
