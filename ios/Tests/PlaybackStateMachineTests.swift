import XCTest
@testable import RnVlcPlyr

final class PlaybackStateMachineTests: XCTestCase {
  private let networkUri = "https://host/video.mp4"

  private func playingMachine(uri: String = "https://host/video.mp4") -> PlaybackStateMachine {
    var machine = PlaybackStateMachine()
    _ = machine.load(uri: uri, autoPlay: true)
    _ = machine.handle(engine: .playing, engineIsPlaying: true, loop: false)
    return machine
  }

  func testStartsIdleWithoutSource() {
    let machine = PlaybackStateMachine()
    XCTAssertEqual(machine.status, .idle)
    XCTAssertEqual(machine.playAction(), .none)
    XCTAssertFalse(machine.canStop)
  }

  func testLoadWithAutoPlayEmitsOpeningThenPlayingThenLoadedOnce() {
    var machine = PlaybackStateMachine()
    XCTAssertEqual(machine.load(uri: networkUri, autoPlay: true), [.status(.opening)])
    XCTAssertEqual(machine.handle(engine: .opening, engineIsPlaying: false, loop: false), [])
    XCTAssertEqual(machine.handle(engine: .buffering, engineIsPlaying: false, loop: false), [.status(.buffering)])
    XCTAssertEqual(machine.handle(engine: .playing, engineIsPlaying: true, loop: false), [.status(.playing), .loaded])
    XCTAssertEqual(machine.handle(engine: .paused, engineIsPlaying: false, loop: false), [.status(.paused)])
    XCTAssertEqual(machine.handle(engine: .playing, engineIsPlaying: true, loop: false), [.status(.playing)])
  }

  func testAutoPlayOffStaysIdleUntilPlay() {
    var machine = PlaybackStateMachine()
    _ = machine.load(uri: networkUri, autoPlay: false)
    XCTAssertEqual(machine.status, .idle)
    XCTAssertEqual(machine.handle(engine: .buffering, engineIsPlaying: false, loop: false), [])
    XCTAssertEqual(machine.playAction(), .start)
  }

  func testBufferingIgnoredWhilePlaying() {
    var machine = playingMachine()
    XCTAssertEqual(machine.handle(engine: .buffering, engineIsPlaying: true, loop: false), [])
    XCTAssertEqual(machine.handle(engine: .buffering, engineIsPlaying: false, loop: false), [])
    XCTAssertEqual(machine.status, .playing)
  }

  func testBufferingIgnoredWhenEngineReportsPlaying() {
    var machine = PlaybackStateMachine()
    _ = machine.load(uri: networkUri, autoPlay: false)
    _ = machine.beginPlayback()
    XCTAssertEqual(machine.handle(engine: .buffering, engineIsPlaying: true, loop: false), [])
    XCTAssertEqual(machine.status, .opening)
  }

  func testBufferingWhilePausedKeepsPaused() {
    var machine = playingMachine()
    _ = machine.handle(engine: .paused, engineIsPlaying: false, loop: false)
    XCTAssertEqual(machine.handle(engine: .buffering, engineIsPlaying: false, loop: false), [])
    XCTAssertEqual(machine.status, .paused)
  }

  func testEndedWithoutLoopEmitsEndedStatusThenEvent() {
    var machine = playingMachine()
    XCTAssertEqual(machine.handle(engine: .ended, engineIsPlaying: false, loop: false), [.status(.ended), .ended])
    XCTAssertEqual(machine.handle(engine: .stopped, engineIsPlaying: false, loop: false), [])
    XCTAssertEqual(machine.playAction(), .restart)
  }

  func testEngineStoppedIsIgnored() {
    var machine = playingMachine()
    XCTAssertEqual(machine.handle(engine: .stopped, engineIsPlaying: false, loop: false), [])
    XCTAssertEqual(machine.status, .playing)
  }

  func testInvalidSourceWhileInErrorEmitsNothing() {
    var machine = PlaybackStateMachine()
    _ = machine.failInvalidSource()
    XCTAssertEqual(machine.failInvalidSource(), [])
    XCTAssertEqual(machine.status, .error)
  }

  func testLoopRestartFlagIsExposed() {
    var machine = playingMachine()
    _ = machine.handle(engine: .ended, engineIsPlaying: false, loop: true)
    XCTAssertTrue(machine.isRestartingForLoop)
    _ = machine.handle(engine: .playing, engineIsPlaying: true, loop: true)
    XCTAssertFalse(machine.isRestartingForLoop)
  }

  func testEndedWithLoopRestartsWithoutStatusOrEvent() {
    var machine = playingMachine()
    XCTAssertEqual(machine.handle(engine: .ended, engineIsPlaying: false, loop: true), [.restartForLoop])
    XCTAssertEqual(machine.handle(engine: .stopped, engineIsPlaying: false, loop: true), [])
    XCTAssertEqual(machine.handle(engine: .opening, engineIsPlaying: false, loop: true), [])
    XCTAssertEqual(machine.handle(engine: .buffering, engineIsPlaying: false, loop: true), [])
    XCTAssertEqual(machine.handle(engine: .playing, engineIsPlaying: true, loop: true), [])
    XCTAssertEqual(machine.status, .playing)
    XCTAssertEqual(machine.handle(engine: .ended, engineIsPlaying: false, loop: true), [.restartForLoop])
  }

  func testRequestedStopEmitsStoppedAndIgnoresEngineStop() {
    var machine = playingMachine()
    XCTAssertTrue(machine.canStop)
    XCTAssertEqual(machine.requestStop(), [.status(.stopped)])
    XCTAssertEqual(machine.handle(engine: .stopped, engineIsPlaying: false, loop: false), [])
    XCTAssertEqual(machine.handle(engine: .buffering, engineIsPlaying: false, loop: false), [])
    XCTAssertEqual(machine.status, .stopped)
    XCTAssertEqual(machine.playAction(), .restart)
  }

  func testNetworkErrorBeforePlaying() {
    var machine = PlaybackStateMachine()
    _ = machine.load(uri: networkUri, autoPlay: false)
    _ = machine.beginPlayback()
    XCTAssertEqual(
      machine.handle(engine: .error, engineIsPlaying: false, loop: false),
      [.status(.error), .error(PlayerErrors.network)]
    )
    XCTAssertEqual(machine.playAction(), .reload)
    XCTAssertEqual(machine.handle(engine: .stopped, engineIsPlaying: false, loop: false), [])
  }

  func testMediaErrorAfterPlaying() {
    var machine = playingMachine()
    XCTAssertEqual(
      machine.handle(engine: .error, engineIsPlaying: false, loop: false),
      [.status(.error), .error(PlayerErrors.media)]
    )
  }

  func testInvalidSource() {
    var machine = PlaybackStateMachine()
    XCTAssertEqual(machine.failInvalidSource(), [.status(.error), .error(PlayerErrors.invalidSource)])
    XCTAssertEqual(machine.playAction(), .reload)
  }

  func testReloadAfterErrorFiresLoadedAgain() {
    var machine = PlaybackStateMachine()
    _ = machine.load(uri: networkUri, autoPlay: false)
    _ = machine.beginPlayback()
    _ = machine.handle(engine: .error, engineIsPlaying: false, loop: false)
    XCTAssertEqual(machine.reloadForRetry(), [.status(.opening)])
    XCTAssertEqual(machine.handle(engine: .playing, engineIsPlaying: true, loop: false), [.status(.playing), .loaded])
  }

  func testLoadedOncePerSource() {
    var machine = playingMachine()
    _ = machine.requestStop()
    _ = machine.beginPlayback()
    XCTAssertEqual(machine.handle(engine: .playing, engineIsPlaying: true, loop: false), [.status(.playing)])
    XCTAssertEqual(machine.load(uri: networkUri, autoPlay: true), [.status(.opening)])
    XCTAssertEqual(machine.handle(engine: .playing, engineIsPlaying: true, loop: false), [.status(.playing), .loaded])
  }

  func testLoadWithoutAutoPlayFromPlayingGoesIdle() {
    var machine = playingMachine()
    XCTAssertEqual(machine.load(uri: networkUri, autoPlay: false), [.status(.idle)])
  }

  func testClearReturnsToIdle() {
    var machine = playingMachine()
    XCTAssertEqual(machine.clear(), [.status(.idle)])
    XCTAssertFalse(machine.hasSource)
    XCTAssertEqual(machine.handle(engine: .playing, engineIsPlaying: true, loop: false), [])
  }

  func testStatusNeverRepeats() {
    var machine = PlaybackStateMachine()
    _ = machine.load(uri: networkUri, autoPlay: false)
    XCTAssertEqual(machine.beginPlayback(), [.status(.opening)])
    XCTAssertEqual(machine.beginPlayback(), [])
  }

  func testPlayActions() {
    var machine = playingMachine()
    XCTAssertEqual(machine.playAction(), .none)
    _ = machine.handle(engine: .paused, engineIsPlaying: false, loop: false)
    XCTAssertEqual(machine.playAction(), .resume)
  }

  func testPauseOnlyWhenPlayingOrBuffering() {
    var machine = PlaybackStateMachine()
    _ = machine.load(uri: networkUri, autoPlay: false)
    XCTAssertFalse(machine.canPause)
    _ = machine.beginPlayback()
    XCTAssertFalse(machine.canPause)
    _ = machine.handle(engine: .buffering, engineIsPlaying: false, loop: false)
    XCTAssertTrue(machine.canPause)
    _ = machine.handle(engine: .playing, engineIsPlaying: true, loop: false)
    XCTAssertTrue(machine.canPause)
    _ = machine.handle(engine: .paused, engineIsPlaying: false, loop: false)
    XCTAssertFalse(machine.canPause)
  }

  func testSeekActions() {
    var machine = playingMachine()
    XCTAssertEqual(machine.seekAction(targetMs: 5000, duration: 10_000, isSeekable: false), .none)
    XCTAssertEqual(machine.seekAction(targetMs: 50_000, duration: 10_000, isSeekable: true), .seek(10_000))
    XCTAssertEqual(machine.seekAction(targetMs: -5, duration: 10_000, isSeekable: true), .seek(0))
    _ = machine.handle(engine: .ended, engineIsPlaying: false, loop: false)
    XCTAssertEqual(machine.seekAction(targetMs: 5000, duration: 10_000, isSeekable: true), .restartThenSeek(5000))
    _ = machine.requestStop()
    XCTAssertEqual(machine.seekAction(targetMs: 5000, duration: 10_000, isSeekable: true), .restartThenSeek(5000))
  }

  func testIsLiveRequiresPlaying() {
    var machine = PlaybackStateMachine()
    _ = machine.load(uri: networkUri, autoPlay: false)
    XCTAssertFalse(machine.hasReachedPlaying)
    _ = machine.beginPlayback()
    _ = machine.handle(engine: .playing, engineIsPlaying: true, loop: false)
    XCTAssertTrue(machine.hasReachedPlaying)
  }
}
