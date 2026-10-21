import XCTest
@testable import RnVlcPlyr

final class SubtitleStyleTests: XCTestCase {
  private static let fullStyle = SubtitleStyle(
    fontFamily: "Helvetica",
    fontScale: 200,
    bold: true,
    color: SubtitleColor(rgb: 0xFFFF00),
    backgroundColor: SubtitleColor(rgb: 0x000000, alpha: 0x80),
    outlineColor: SubtitleColor(rgb: 0x101010),
    outlineThickness: .thick,
    shadowColor: SubtitleColor(rgb: 0x202020, alpha: 0x40),
    shadowDistance: 0.1,
    marginBottom: 40
  )

  func testHexColorParsesShortLongAndAlphaForms() {
    XCTAssertEqual(SubtitleColor(hex: "#ff0"), SubtitleColor(rgb: 0xFFFF00))
    XCTAssertEqual(SubtitleColor(hex: "#FF8800"), SubtitleColor(rgb: 0xFF8800))
    XCTAssertEqual(SubtitleColor(hex: " #ff880080 "), SubtitleColor(rgb: 0xFF8800, alpha: 0x80))
  }

  func testInvalidHexColorIsNil() {
    ["ff8800", "#ff88", "#GG0000", "", "#", "#ff880080ff", "red"].forEach {
      XCTAssertNil(SubtitleColor(hex: $0), $0)
    }
  }

  func testHexColorIsCanonicalUppercaseWithAlphaOnlyWhenTranslucent() {
    XCTAssertEqual(SubtitleColor(rgb: 0xFF8800).hex, "#FF8800")
    XCTAssertEqual(SubtitleColor(rgb: 0x0000FF, alpha: 0x80).hex, "#0000FF80")
  }

  func testFontScaleRoundsClampsAndMapsToRelativeFontSize() {
    XCTAssertEqual(VlcContract.clampFontScale(10), 25)
    XCTAssertEqual(VlcContract.clampFontScale(1000), 400)
    XCTAssertEqual(VlcContract.clampFontScale(149.5), 150)
    XCTAssertNil(VlcContract.clampFontScale(.nan))
    XCTAssertEqual(VlcContract.relativeFontSize(forScale: 100), 16)
    XCTAssertEqual(VlcContract.relativeFontSize(forScale: 200), 8)
    XCTAssertEqual(VlcContract.relativeFontSize(forScale: 50), 32)
    XCTAssertEqual(VlcContract.relativeFontSize(forScale: 150), 11)
    XCTAssertEqual(VlcContract.relativeFontSize(forScale: 25), 64)
    XCTAssertEqual(VlcContract.relativeFontSize(forScale: 400), 4)
  }

  func testShadowDistanceAndMarginClamp() {
    XCTAssertEqual(VlcContract.clampShadowDistance(-1), 0)
    XCTAssertEqual(VlcContract.clampShadowDistance(2), 1)
    XCTAssertEqual(VlcContract.clampShadowDistance(0.25), 0.25)
    XCTAssertNil(VlcContract.clampShadowDistance(.nan))
    XCTAssertEqual(VlcContract.clampSubtitleMargin(-5), 0)
    XCTAssertEqual(VlcContract.clampSubtitleMargin(5000), 1000)
    XCTAssertEqual(VlcContract.clampSubtitleMargin(12.5), 13)
    XCTAssertNil(VlcContract.clampSubtitleMargin(.nan))
  }

  func testFontFamilyIsTrimmedAndBlankIsNil() {
    XCTAssertEqual(VlcContract.fontFamily("  Avenir Next "), "Avenir Next")
    XCTAssertNil(VlcContract.fontFamily("   "))
    XCTAssertNil(VlcContract.fontFamily(nil))
  }

  func testOptionsAreEmptyWithoutStyle() {
    XCTAssertEqual(VlcContract.subtitleStyleOptions(nil), [])
    XCTAssertEqual(VlcContract.subtitleStyleOptions(SubtitleStyle()), [])
    XCTAssertNil(SubtitleStyle().nonEmpty)
  }

  func testOptionsFollowFixedOrderAndFormat() {
    XCTAssertEqual(VlcContract.subtitleStyleOptions(Self.fullStyle), [
      "--freetype-font=Helvetica",
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
      "--sub-margin=40",
    ])
  }

  func testOptionsOmitUnsetFieldsAndFalseBold() {
    let style = SubtitleStyle(bold: false, outlineThickness: SubtitleOutline.none)
    XCTAssertEqual(VlcContract.subtitleStyleOptions(style), ["--freetype-outline-thickness=0"])
  }

  func testPayloadIsNormalisedAndInvalidFieldsDropped() {
    let thin: VlcOutlineThickness? = VlcOutlineThickness(fromString: "thin")
    let notANumber: Double? = .nan
    let payload = VlcSubtitleStyle(
      fontFamily: " ", fontScale: 1000, bold: nil, color: "#ff0", backgroundColor: "nope",
      outlineColor: nil, outlineThickness: thin, shadowColor: nil, shadowDistance: notANumber, marginBottom: 12.4
    )
    let style = PlayerPayloads.subtitleStyle(payload)
    XCTAssertEqual(style, SubtitleStyle(fontScale: 400, color: SubtitleColor(rgb: 0xFFFF00), outlineThickness: .thin, marginBottom: 12))
    let roundTrip = style.map(PlayerPayloads.subtitleStyle)
    XCTAssertEqual(roundTrip?.color, "#FFFF00")
    XCTAssertEqual(roundTrip?.outlineThickness?.stringValue, "thin")
    XCTAssertEqual(roundTrip?.fontScale, 400)
  }

  func testPayloadWithOnlyInvalidFieldsIsNil() {
    let payload = VlcSubtitleStyle(
      fontFamily: "", fontScale: Double.nan, bold: nil, color: "blue", backgroundColor: nil,
      outlineColor: nil, outlineThickness: nil, shadowColor: nil, shadowDistance: nil, marginBottom: nil
    )
    XCTAssertNil(PlayerPayloads.subtitleStyle(payload))
  }
}
