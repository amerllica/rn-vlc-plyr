package com.margelo.nitro.rnvlcplyr.contract

import java.net.URL

object ArtworkRedirect {
  const val MAX_HOPS = 5

  private const val HTTP = "http"
  private const val HTTPS = "https"
  private val REDIRECT_STATUSES = setOf(301, 302, 303, 307, 308)

  fun isRedirect(status: Int): Boolean = status in REDIRECT_STATUSES

  fun target(from: String, location: String?): String? {
    if (location == null) return null
    val origin = runCatching { URL(from) }.getOrNull() ?: return null
    val resolved = runCatching { URL(origin, location) }.getOrNull() ?: return null
    return resolved.toString().takeIf { allows(origin.protocol.lowercase(), resolved.protocol.lowercase()) }
  }

  private fun allows(from: String, to: String): Boolean = to == HTTPS || (to == HTTP && from == HTTP)
}
