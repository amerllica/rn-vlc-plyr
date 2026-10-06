import XCTest
@testable import RnVlcPlyr

final class ResizeGeometryTests: XCTestCase {
  private let viewSize = CGSize(width: 390, height: 219.375)

  func testContainFitsWithoutOverrides() {
    XCTAssertEqual(ResizeGeometry.settings(mode: .contain, viewSize: viewSize), .fit)
  }

  func testCoverCropsToViewRatio() {
    XCTAssertEqual(ResizeGeometry.settings(mode: .cover, viewSize: CGSize(width: 400, height: 300)), ResizeSettings(scaleFactor: 0, aspectRatio: nil, cropGeometry: "4:3"))
  }

  func testStretchForcesViewAspectRatio() {
    XCTAssertEqual(ResizeGeometry.settings(mode: .stretch, viewSize: CGSize(width: 1920, height: 1080)), ResizeSettings(scaleFactor: 0, aspectRatio: "16:9", cropGeometry: nil))
  }

  func testOriginalUsesNativeScale() {
    XCTAssertEqual(ResizeGeometry.settings(mode: .original, viewSize: viewSize), ResizeSettings(scaleFactor: 1, aspectRatio: nil, cropGeometry: nil))
  }

  func testZeroSizedViewFallsBackToFit() {
    XCTAssertEqual(ResizeGeometry.settings(mode: .cover, viewSize: .zero), .fit)
    XCTAssertNil(ResizeGeometry.ratio(of: CGSize(width: 10, height: 0)))
  }
}

final class SurfaceStackTests: XCTestCase {
  func testMostRecentAttachedIsTopAndDetachRestoresPrevious() {
    let first = NSObject()
    let second = NSObject()
    var stack = SurfaceStack<NSObject>()
    XCTAssertTrue(stack.isEmpty)
    stack.attach(first)
    stack.attach(second)
    XCTAssertTrue(stack.top === second)
    stack.detach(second)
    XCTAssertTrue(stack.top === first)
    stack.attach(second)
    stack.attach(first)
    XCTAssertTrue(stack.top === first)
    stack.detach(first)
    XCTAssertTrue(stack.top === second)
  }

  func testReleasedElementsAreSkipped() {
    let kept = NSObject()
    var stack = SurfaceStack<NSObject>()
    stack.attach(kept)
    autoreleasepool {
      let transient = NSObject()
      stack.attach(transient)
    }
    XCTAssertTrue(stack.top === kept)
  }
}

final class ListenerRegistryTests: XCTestCase {
  func testEmitsInOrderAndRemoveIsIdempotent() throws {
    let player = HybridVlcPlayer()
    defer { try? player.release() }
    var received: [String] = []
    let first = try player.addOnVolumeChangeListener { received.append("a\(Int($0.volume))") }
    let second = try player.addOnVolumeChangeListener { received.append("b\(Int($0.volume))") }
    player.volume = 10
    first.remove()
    first.remove()
    player.volume = 20
    second.remove()
    player.volume = 30
    XCTAssertEqual(received, ["a10", "b10", "b20"])
  }

  func testListenerMayRemoveItselfWhileEmitting() throws {
    let player = HybridVlcPlayer()
    defer { try? player.release() }
    var subscription: VlcListenerSubscription?
    var calls = 0
    subscription = try player.addOnVolumeChangeListener { _ in
      calls += 1
      subscription?.remove()
    }
    player.volume = 10
    player.volume = 20
    XCTAssertEqual(calls, 1)
  }
}
