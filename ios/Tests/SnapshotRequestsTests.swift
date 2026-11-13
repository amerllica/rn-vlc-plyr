import NitroModules
import XCTest
@testable import RnVlcPlyr

final class SnapshotRequestsTests: XCTestCase {
  private let requests = SnapshotRequests()
  private var path = ""

  override func setUp() {
    super.setUp()
    path = SnapshotRequests.makeTemporaryPath()
  }

  override func tearDown() {
    try? FileManager.default.removeItem(atPath: path)
    super.tearDown()
  }

  private func trackedPromise(settledBy expectation: XCTestExpectation, resolved: @escaping (String?) -> Void) -> Promise<String> {
    let promise = Promise<String>()
    promise.then { uri in
      resolved(uri)
      expectation.fulfill()
    }
    promise.catch { _ in
      resolved(nil)
      expectation.fulfill()
    }
    requests.track(path: path, promise: promise)
    return promise
  }

  func testAWrittenFileResolvesWithItsFileUri() throws {
    let settled = expectation(description: "snapshot settled")
    var resolvedUri: String?
    _ = trackedPromise(settledBy: settled) { resolvedUri = $0 }
    try Data([1, 2, 3]).write(to: URL(fileURLWithPath: path))

    requests.engineDidTakeSnapshot()

    wait(for: [settled], timeout: 5)
    XCTAssertEqual(resolvedUri, URL(fileURLWithPath: path).absoluteString)
  }

  func testAFailedRequestRejectsAndDeletesTheEmptyFile() {
    let settled = expectation(description: "snapshot settled")
    var resolvedUri: String? = "unsettled"
    _ = trackedPromise(settledBy: settled) { resolvedUri = $0 }
    FileManager.default.createFile(atPath: path, contents: Data())

    requests.engineDidFinishRequest(path: path, succeeded: false)

    wait(for: [settled], timeout: 5)
    XCTAssertNil(resolvedUri)
    XCTAssertFalse(FileManager.default.fileExists(atPath: path))
  }

  func testASucceededRequestWaitsForTheFile() throws {
    let settled = expectation(description: "snapshot settled")
    var resolvedUri: String?
    _ = trackedPromise(settledBy: settled) { resolvedUri = $0 }

    requests.engineDidFinishRequest(path: path, succeeded: true)
    try Data([1]).write(to: URL(fileURLWithPath: path))
    requests.engineDidTakeSnapshot()

    wait(for: [settled], timeout: 5)
    XCTAssertEqual(resolvedUri, URL(fileURLWithPath: path).absoluteString)
  }

  func testRejectAllRejectsEveryPendingRequest() {
    let settled = expectation(description: "snapshot settled")
    var resolvedUri: String? = "unsettled"
    _ = trackedPromise(settledBy: settled) { resolvedUri = $0 }

    requests.rejectAll()

    wait(for: [settled], timeout: 5)
    XCTAssertNil(resolvedUri)
  }
}
