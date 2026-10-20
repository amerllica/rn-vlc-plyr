package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcOutlineThickness
import com.margelo.nitro.rnvlcplyr.VlcSubtitleStyle
import java.util.Locale

data class SubtitleColor(val rgb: Int, val alpha: Int = OPAQUE) {
  val hex: String
    get() = if (alpha == OPAQUE) String.format(Locale.ROOT, "#%06X", rgb) else String.format(Locale.ROOT, "#%06X%02X", rgb, alpha)

  companion object {
    const val OPAQUE = 255
    private const val HEX_RADIX = 16
    private const val SHORT_LENGTH = 3
    private const val RGBA_LENGTH = 8
    private const val ALPHA_BITS = 8
    private val HEX_PATTERN = Regex("#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})")

    fun parse(value: String): SubtitleColor? {
      val digits = HEX_PATTERN.matchEntire(value.trim())?.groupValues?.get(1) ?: return null
      val expanded = if (digits.length == SHORT_LENGTH) digits.map { "$it$it" }.joinToString("") else digits
      val number = expanded.toLong(HEX_RADIX)
      return if (expanded.length == RGBA_LENGTH) {
        SubtitleColor((number shr ALPHA_BITS).toInt(), (number and OPAQUE.toLong()).toInt())
      } else {
        SubtitleColor(number.toInt())
      }
    }
  }
}

object SubtitleStyles {
  private const val MIN_FONT_SCALE = 25L
  private const val MAX_FONT_SCALE = 400L
  private const val MIN_SUBTITLE_MARGIN = 0L
  private const val MAX_SUBTITLE_MARGIN = 1000L
  private const val MIN_SHADOW_DISTANCE = 0.0
  private const val MAX_SHADOW_DISTANCE = 1.0
  private const val DEFAULT_RELATIVE_FONT_SIZE = 16
  private const val FULL_FONT_SCALE = 100

  private val EMPTY = VlcSubtitleStyle(
    fontFamily = null,
    fontScale = null,
    bold = null,
    color = null,
    backgroundColor = null,
    outlineColor = null,
    outlineThickness = null,
    shadowColor = null,
    shadowDistance = null,
    marginBottom = null
  )

  fun normalize(style: VlcSubtitleStyle?): VlcSubtitleStyle? {
    if (style == null) return null
    val normalized = VlcSubtitleStyle(
      fontFamily = style.fontFamily?.trim()?.takeIf { it.isNotEmpty() },
      fontScale = style.fontScale.finite()?.let { clampFontScale(it).toDouble() },
      bold = style.bold,
      color = style.color?.let(::canonicalColor),
      backgroundColor = style.backgroundColor?.let(::canonicalColor),
      outlineColor = style.outlineColor?.let(::canonicalColor),
      outlineThickness = style.outlineThickness,
      shadowColor = style.shadowColor?.let(::canonicalColor),
      shadowDistance = style.shadowDistance.finite()?.coerceIn(MIN_SHADOW_DISTANCE, MAX_SHADOW_DISTANCE),
      marginBottom = style.marginBottom.finite()?.let { clampSubtitleMargin(it).toDouble() }
    )
    return normalized.takeUnless { it == EMPTY }
  }

  fun clampFontScale(value: Double): Int = VlcContract.roundHalfUp(value).coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE).toInt()

  fun clampSubtitleMargin(value: Double): Int =
    VlcContract.roundHalfUp(value).coerceIn(MIN_SUBTITLE_MARGIN, MAX_SUBTITLE_MARGIN).toInt()

  fun relativeFontSize(fontScale: Int): Int =
    VlcContract.roundHalfUp(DEFAULT_RELATIVE_FONT_SIZE * FULL_FONT_SCALE.toDouble() / fontScale).toInt()

  fun outlinePixels(thickness: VlcOutlineThickness): Int = when (thickness) {
    VlcOutlineThickness.NONE -> 0
    VlcOutlineThickness.THIN -> 2
    VlcOutlineThickness.NORMAL -> 4
    VlcOutlineThickness.THICK -> 6
  }

  fun options(style: VlcSubtitleStyle?): List<String> {
    if (style == null) return emptyList()
    return buildList {
      style.fontFamily?.let { add("--freetype-font=$it") }
      style.fontScale?.let { add("--freetype-rel-fontsize=${relativeFontSize(it.toInt())}") }
      if (style.bold == true) add("--freetype-bold")
      addColor(style.color, "freetype-color", "freetype-opacity")
      addColor(style.backgroundColor, "freetype-background-color", "freetype-background-opacity")
      addColor(style.outlineColor, "freetype-outline-color", "freetype-outline-opacity")
      style.outlineThickness?.let { add("--freetype-outline-thickness=${outlinePixels(it)}") }
      addColor(style.shadowColor, "freetype-shadow-color", "freetype-shadow-opacity")
      style.shadowDistance?.let { add("--freetype-shadow-distance=$it") }
      style.marginBottom?.let { add("--sub-margin=${it.toInt()}") }
    }
  }

  private fun canonicalColor(value: String): String? = SubtitleColor.parse(value)?.hex

  private fun Double?.finite(): Double? = this?.takeUnless { it.isNaN() }

  private fun MutableList<String>.addColor(value: String?, color: String, opacity: String) {
    val parsed = value?.let(SubtitleColor::parse) ?: return
    add("--$color=${parsed.rgb}")
    add("--$opacity=${parsed.alpha}")
  }
}
