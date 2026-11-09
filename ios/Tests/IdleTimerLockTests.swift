import XCTest
@testable import RnVlcPlyr

final class IdleTimerLockTests: XCTestCase {
  private let first = NSObject()
  private let second = NSObject()

  func testDisablesTheIdleTimerWhileAnyPlayerIsActive() {
    var lock = IdleTimerLock()
    XCTAssertEqual(lock.update(ObjectIdentifier(first), keepsAwake: true, isDisabled: false), true)
    XCTAssertNil(lock.update(ObjectIdentifier(second), keepsAwake: true, isDisabled: true))
    XCTAssertNil(lock.update(ObjectIdentifier(first), keepsAwake: false, isDisabled: true))
    XCTAssertEqual(lock.update(ObjectIdentifier(second), keepsAwake: false, isDisabled: true), false)
  }

  func testNeverReleasesATimerTheAppDisabled() {
    var lock = IdleTimerLock()
    XCTAssertNil(lock.update(ObjectIdentifier(first), keepsAwake: true, isDisabled: true))
    XCTAssertNil(lock.update(ObjectIdentifier(first), keepsAwake: false, isDisabled: true))
  }

  func testRepeatedStatusesChangeNothing() {
    var lock = IdleTimerLock()
    XCTAssertEqual(lock.update(ObjectIdentifier(first), keepsAwake: true, isDisabled: false), true)
    XCTAssertNil(lock.update(ObjectIdentifier(first), keepsAwake: true, isDisabled: true))
    XCTAssertEqual(lock.update(ObjectIdentifier(first), keepsAwake: false, isDisabled: true), false)
    XCTAssertNil(lock.update(ObjectIdentifier(first), keepsAwake: false, isDisabled: false))
  }
}
