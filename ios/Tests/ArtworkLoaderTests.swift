import XCTest
import UIKit
@testable import RnVlcPlyr

final class ArtworkLoaderTests: XCTestCase {
  private static let bytesOverTheLimit = 10 * 1024 * 1024 + 1
  private static let remoteUri = "https://artwork.test/cover.png"
  private var directory: URL!
  private var attempts = 0
  private var scheduledDelays: [Int] = []
  private var warnings: [String] = []
  private var loads: XCTestExpectation?
  private var loader: ArtworkLoader!

  override func setUp() {
    super.setUp()
    directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
    try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
    loader = ArtworkLoader(
      schedule: { [unowned self] delayMs, work in
        self.scheduledDelays.append(delayMs)
        work()
      },
      fetch: { [unowned self] uri, completion in
        self.attempts += 1
        ArtworkLoader.fetch(uri, completion: completion)
      },
      log: { [unowned self] message in self.warnings.append(message) },
      onLoad: { [unowned self] in self.loads?.fulfill() }
    )
  }

  override func tearDown() {
    loader = nil
    try? FileManager.default.removeItem(at: directory)
    super.tearDown()
  }

  func testFileArtworkLoadsAndCaches() throws {
    let uri = try writePng(named: "cover.png").absoluteString

    XCTAssertNil(awaitLoad(uri))
    let image = try XCTUnwrap(loader.image(for: uri))
    XCTAssertTrue(loader.image(for: uri) === image)
    XCTAssertEqual(attempts, 1)
    XCTAssertEqual(scheduledDelays, [])
    XCTAssertEqual(warnings, [])
  }

  func testMissingArtworkRetriesThenGivesUp() {
    let uri = directory.appendingPathComponent("missing.png").absoluteString

    XCTAssertNil(awaitLoad(uri))
    XCTAssertEqual(attempts, 4)
    XCTAssertEqual(scheduledDelays, [1000, 3000, 9000])
    XCTAssertEqual(warnings, ["rn-vlc-plyr: artwork could not be loaded from \(uri): file not found"])
    XCTAssertNil(loader.image(for: uri))
    XCTAssertEqual(attempts, 4)
  }

  func testResetAllowsANewAttempt() throws {
    let missing = directory.appendingPathComponent("late.png")
    let uri = missing.absoluteString
    XCTAssertNil(awaitLoad(uri))
    XCTAssertEqual(attempts, 4)

    _ = try writePng(named: "late.png")
    XCTAssertNil(loader.image(for: uri))
    XCTAssertEqual(attempts, 4)

    loader.reset(uri)
    XCTAssertNil(awaitLoad(uri))
    XCTAssertEqual(attempts, 5)
    XCTAssertNotNil(loader.image(for: uri))
    XCTAssertEqual(warnings.count, 1)
  }

  func testFileOverTheSizeLimitIsRefused() throws {
    let url = try writePng(named: "huge.png")
    let handle = try FileHandle(forWritingTo: url)
    handle.seekToEndOfFile()
    handle.write(Data(count: Self.bytesOverTheLimit))
    try handle.close()

    XCTAssertNil(awaitLoad(url.absoluteString))

    XCTAssertNil(loader.image(for: url.absoluteString))
    XCTAssertEqual(warnings, ["rn-vlc-plyr: artwork could not be loaded from \(url.absoluteString): too large"])
  }

  func testHttpArtworkLoads() throws {
    let png = try Data(contentsOf: writePng(named: "remote.png"))

    let result = try download(replyingWith: 200, body: png)

    XCTAssertNotNil(try? result.get())
  }

  func testRedirectsStayOnHttpAndStopAfterFiveHops() {
    let http = URL(string: "http://host/a.png")
    let https = URL(string: "https://host/a.png")
    XCTAssertTrue(ArtworkLoader.allowsRedirect(from: http, to: https, hop: 1))
    XCTAssertTrue(ArtworkLoader.allowsRedirect(from: http, to: http, hop: 5))
    XCTAssertFalse(ArtworkLoader.allowsRedirect(from: https, to: http, hop: 1))
    XCTAssertFalse(ArtworkLoader.allowsRedirect(from: https, to: URL(string: "file:///a.png"), hop: 1))
    XCTAssertFalse(ArtworkLoader.allowsRedirect(from: https, to: https, hop: 6))
  }

  func testHttpErrorStatusFails() throws {
    XCTAssertEqual(failure(of: try download(replyingWith: 404, body: Data())), .http(404))
  }

  func testHttpBodyOverTheSizeLimitFails() throws {
    XCTAssertEqual(failure(of: try download(replyingWith: 200, body: Data(count: Self.bytesOverTheLimit))), .tooLarge)
  }

  private func download(replyingWith status: Int, body: Data) throws -> Result<UIImage, ArtworkFailure> {
    let url = try XCTUnwrap(URL(string: Self.remoteUri))
    StubArtworkServer.reply = (status, body)
    let configuration = URLSessionConfiguration.ephemeral
    configuration.protocolClasses = [StubArtworkServer.self]
    let finished = expectation(description: "download finished")
    var result: Result<UIImage, ArtworkFailure> = .failure(.timedOut)
    ArtworkLoader.download(url, configuration: configuration) { downloaded in
      result = downloaded
      finished.fulfill()
    }
    wait(for: [finished], timeout: 5)
    return result
  }

  private func failure(of result: Result<UIImage, ArtworkFailure>) -> ArtworkFailure? {
    guard case .failure(let failure) = result else { return nil }
    return failure
  }

  private func awaitLoad(_ uri: String) -> UIImage? {
    let expectation = expectation(description: "artwork settled")
    loads = expectation
    let immediate = loader.image(for: uri)
    wait(for: [expectation], timeout: 5)
    loads = nil
    return immediate
  }

  private func writePng(named name: String) throws -> URL {
    let renderer = UIGraphicsImageRenderer(size: CGSize(width: 4, height: 4))
    let image = renderer.image { context in
      UIColor.red.setFill()
      context.fill(CGRect(x: 0, y: 0, width: 4, height: 4))
    }
    let url = directory.appendingPathComponent(name)
    try XCTUnwrap(image.pngData()).write(to: url)
    return url
  }
}

private final class StubArtworkServer: URLProtocol {
  static var reply: (status: Int, body: Data) = (200, Data())

  override class func canInit(with request: URLRequest) -> Bool {
    true
  }

  override class func canonicalRequest(for request: URLRequest) -> URLRequest {
    request
  }

  override func startLoading() {
    guard let url = request.url,
          let response = HTTPURLResponse(url: url, statusCode: Self.reply.status, httpVersion: nil, headerFields: nil) else { return }
    client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
    client?.urlProtocol(self, didLoad: Self.reply.body)
    client?.urlProtocolDidFinishLoading(self)
  }

  override func stopLoading() {}
}
