import XCTest
import UIKit
@testable import RnVlcPlyr

final class ArtworkLoaderTests: XCTestCase {
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
