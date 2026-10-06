import XCTest
@testable import RnVlcPlyr

final class MediaRequestTests: XCTestCase {
  func testEmptyUriIsInvalid() {
    XCTAssertNil(MediaRequest.make(uri: ""))
  }

  func testUriWithoutSchemeIsInvalid() {
    XCTAssertNil(MediaRequest.make(uri: "video.mp4"))
    XCTAssertNil(MediaRequest.make(uri: "relative/path.mp4"))
  }

  func testAbsolutePathBecomesFileUrl() {
    XCTAssertEqual(MediaRequest.make(uri: "/tmp/video.mp4")?.url.absoluteString, "file:///tmp/video.mp4")
  }

  func testSchemedUrisAreKept() {
    XCTAssertEqual(MediaRequest.make(uri: "https://host/a.m3u8")?.url.absoluteString, "https://host/a.m3u8")
    XCTAssertEqual(MediaRequest.make(uri: "rtsp://cam/stream")?.url.absoluteString, "rtsp://cam/stream")
    XCTAssertEqual(MediaRequest.make(uri: "file:///tmp/a.mp4")?.url.absoluteString, "file:///tmp/a.mp4")
  }

  func testOptionsAreUserAgentThenReferrerThenVerbatim() {
    let request = MediaRequest.make(uri: "http://host/a", userAgent: "UA/1", referrer: "http://ref", vlcOptions: [":network-caching=300", "--no-audio"])
    XCTAssertEqual(request?.options, [":http-user-agent=UA/1", ":http-referrer=http://ref", ":network-caching=300", "--no-audio"])
  }

  func testOptionsAreEmptyWhenNothingGiven() {
    XCTAssertEqual(MediaRequest.make(uri: "http://host/a")?.options, [])
  }

  func testSchemeDetection() {
    XCTAssertEqual(MediaUri.scheme(of: "HTTPS://x"), "https")
    XCTAssertNil(MediaUri.scheme(of: "/abs"))
    XCTAssertNil(MediaUri.scheme(of: "1http://x"))
    XCTAssertTrue(MediaUri.isNetwork("smb://share/a"))
    XCTAssertFalse(MediaUri.isNetwork("file:///a"))
    XCTAssertFalse(MediaUri.isNetwork("/abs/path"))
  }
}

final class PlayerErrorsTests: XCTestCase {
  func testInvalidSourceFailure() {
    XCTAssertEqual(PlayerErrors.invalidSource.code, .invalidSource)
    XCTAssertEqual(PlayerErrors.invalidSource.message, "Invalid source uri")
    XCTAssertEqual(PlayerErrors.network.code, .network)
    XCTAssertEqual(PlayerErrors.media.code, .media)
  }

  func testFixedMessages() {
    XCTAssertEqual(PlayerErrors.invalidSourceMessage, "Invalid source uri")
    XCTAssertEqual(PlayerErrors.addSubtitleWithoutSource, "addSubtitle requires a source")
    XCTAssertEqual(PlayerErrors.noVideoFrame, "No video frame available")
  }

  func testNetworkErrorBeforeFirstPlayingOnNetworkScheme() {
    for scheme in ["http", "https", "rtsp", "rtmp", "rtp", "udp", "mms", "ftp", "smb"] {
      let error = PlayerErrors.engineFailure(uri: "\(scheme)://host/a", hasReachedPlaying: false)
      XCTAssertEqual(error.code, .network, scheme)
      XCTAssertEqual(error.message, "Could not open the media over the network")
    }
  }

  func testMediaErrorAfterPlayingOrForLocalFiles() {
    let afterPlaying = PlayerErrors.engineFailure(uri: "https://host/a", hasReachedPlaying: true)
    XCTAssertEqual(afterPlaying.code, .media)
    XCTAssertEqual(afterPlaying.message, "The media could not be played")
    XCTAssertEqual(PlayerErrors.engineFailure(uri: "file:///a.mp4", hasReachedPlaying: false).code, .media)
    XCTAssertEqual(PlayerErrors.engineFailure(uri: "/a.mp4", hasReachedPlaying: false).code, .media)
  }
}
