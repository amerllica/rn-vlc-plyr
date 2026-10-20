package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcOutlineThickness
import com.margelo.nitro.rnvlcplyr.VlcSubtitleStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleStylesTest {
  private fun style(
    fontFamily: String? = null,
    fontScale: Double? = null,
    bold: Boolean? = null,
    color: String? = null,
    backgroundColor: String? = null,
    outlineColor: String? = null,
    outlineThickness: VlcOutlineThickness? = null,
    shadowColor: String? = null,
    shadowDistance: Double? = null,
    marginBottom: Double? = null
  ) = VlcSubtitleStyle(
    fontFamily = fontFamily,
    fontScale = fontScale,
    bold = bold,
    color = color,
    backgroundColor = backgroundColor,
    outlineColor = outlineColor,
    outlineThickness = outlineThickness,
    shadowColor = shadowColor,
    shadowDistance = shadowDistance,
    marginBottom = marginBottom
  )

  @Test
  fun hexColorParsesShortLongAndAlphaForms() {
    assertEquals(SubtitleColor(0xFFFF00), SubtitleColor.parse("#ff0"))
    assertEquals(SubtitleColor(0xFF8800), SubtitleColor.parse("#FF8800"))
    assertEquals(SubtitleColor(0xFF8800, 0x80), SubtitleColor.parse(" #ff880080 "))
  }

  @Test
  fun invalidHexColorIsNull() {
    listOf("ff8800", "#ff88", "#GG0000", "", "#", "#ff880080ff", "red").forEach {
      assertNull(it, SubtitleColor.parse(it))
    }
  }

  @Test
  fun hexColorIsCanonicalUppercaseWithAlphaOnlyWhenTranslucent() {
    assertEquals("#FF8800", SubtitleColor(0xFF8800).hex)
    assertEquals("#0000FF80", SubtitleColor(0x0000FF, 0x80).hex)
  }

  @Test
  fun fontScaleRoundsClampsAndMapsToRelativeFontSize() {
    assertEquals(25, SubtitleStyles.clampFontScale(10.0))
    assertEquals(400, SubtitleStyles.clampFontScale(1000.0))
    assertEquals(150, SubtitleStyles.clampFontScale(149.5))
    assertEquals(16, SubtitleStyles.relativeFontSize(100))
    assertEquals(8, SubtitleStyles.relativeFontSize(200))
    assertEquals(32, SubtitleStyles.relativeFontSize(50))
    assertEquals(11, SubtitleStyles.relativeFontSize(150))
    assertEquals(64, SubtitleStyles.relativeFontSize(25))
    assertEquals(4, SubtitleStyles.relativeFontSize(400))
  }

  @Test
  fun marginClamps() {
    assertEquals(0, SubtitleStyles.clampSubtitleMargin(-5.0))
    assertEquals(1000, SubtitleStyles.clampSubtitleMargin(5000.0))
    assertEquals(13, SubtitleStyles.clampSubtitleMargin(12.5))
  }

  @Test
  fun optionsAreEmptyWithoutStyle() {
    assertEquals(emptyList<String>(), SubtitleStyles.options(null))
    assertNull(SubtitleStyles.normalize(style()))
    assertNull(SubtitleStyles.normalize(null))
  }

  @Test
  fun optionsFollowFixedOrderAndFormat() {
    val full = style(
      fontFamily = "Roboto",
      fontScale = 200.0,
      bold = true,
      color = "#FFFF00",
      backgroundColor = "#00000080",
      outlineColor = "#101010",
      outlineThickness = VlcOutlineThickness.THICK,
      shadowColor = "#20202040",
      shadowDistance = 0.1,
      marginBottom = 40.0
    )
    assertEquals(
      listOf(
        "--freetype-font=Roboto",
        "--freetype-rel-fontsize=8",
        "--freetype-bold",
        "--freetype-color=16776960",
        "--freetype-opacity=255",
        "--freetype-background-color=0",
        "--freetype-background-opacity=128",
        "--freetype-outline-color=1052688",
        "--freetype-outline-opacity=255",
        "--freetype-outline-thickness=6",
        "--freetype-shadow-color=2105376",
        "--freetype-shadow-opacity=64",
        "--freetype-shadow-distance=0.1",
        "--sub-margin=40"
      ),
      SubtitleStyles.options(SubtitleStyles.normalize(full))
    )
  }

  @Test
  fun optionsOmitUnsetFieldsAndFalseBold() {
    val options = SubtitleStyles.options(SubtitleStyles.normalize(style(bold = false, outlineThickness = VlcOutlineThickness.NONE)))
    assertEquals(listOf("--freetype-outline-thickness=0"), options)
  }

  @Test
  fun normalizeDropsInvalidFieldsAndClampsNumbers() {
    val normalized = SubtitleStyles.normalize(
      style(
        fontFamily = "  Menlo ",
        fontScale = 5.0,
        color = "#abc",
        backgroundColor = "bad",
        shadowDistance = 3.0,
        marginBottom = Double.NaN
      )
    )
    assertEquals(style(fontFamily = "Menlo", fontScale = 25.0, color = "#AABBCC", shadowDistance = 1.0), normalized)
  }

  @Test
  fun normalizeReturnsNullWhenEveryFieldIsInvalid() {
    assertNull(SubtitleStyles.normalize(style(fontFamily = " ", fontScale = Double.NaN, color = "blue")))
  }

  @Test
  fun settingsReportChangeOnlyForADifferentNormalisedStyle() {
    val settings = PlayerSettings()
    assertTrue(settings.setSubtitleStyle(style(color = "#ff0")))
    assertFalse(settings.setSubtitleStyle(style(color = "#FFFF00")))
    assertEquals("#FFFF00", settings.subtitleStyle?.color)
    assertTrue(settings.setSubtitleStyle(null))
    assertNull(settings.subtitleStyle)
    assertFalse(settings.setSubtitleStyle(style(color = "nope")))
  }
}
