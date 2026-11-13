package com.margelo.nitro.rnvlcplyr.contract

import java.net.URI

object ArtworkRedirect {
  const val MAX_HOPS = 5

  private val REDIRECT_STATUSES = setOf(301, 302, 303, 307, 308)
  private val FOLLOWED_SCHEMES = setOf("http", "https")

  fun isRedirect(status: Int): Boolean = status in REDIRECT_STATUSES

  fun target(from: String, location: String?): String? {
    if (location == null) return null
    val resolved = runCatching { URI(from).resolve(location) }.getOrNull() ?: return null
    return resolved.toString().takeIf { resolved.scheme?.lowercase() in FOLLOWED_SCHEMES }
  }
}
