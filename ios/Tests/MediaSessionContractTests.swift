import XCTest
@testable import RnVlcPlyr

final class BackgroundPolicyTests: XCTestCase {
  func testBackgroundWithStaysActiveDisablesVideo() {
    var policy = BackgroundPolicy()
    XCTAssertEqual(policy.enterBackground(staysActive: true, isActive: true), [.disableVideo])
    XCTAssertTrue(policy.inBackground)
    XCTAssertTrue(policy.videoDisabled)
    var idle = BackgroundPolicy()
    XCTAssertEqual(idle.enterBackground(staysActive: true, isActive: false), [.disableVideo])
  }

  func testBackgroundWithoutStaysActivePausesWhenActive() {
    var policy = BackgroundPolicy()
    XCTAssertEqual(policy.enterBackground(staysActive: false, isActive: true), [.pause])
    XCTAssertFalse(policy.videoDisabled)
  }

  func testBackgroundWithoutStaysActiveDoesNothingWhenInactive() {
    var policy = BackgroundPolicy()
    XCTAssertEqual(policy.enterBackground(staysActive: false, isActive: false), [])
    XCTAssertTrue(policy.inBackground)
  }

  func testEnteringBackgroundTwiceYieldsNothingTheSecondTime() {
    var policy = BackgroundPolicy()
    _ = policy.enterBackground(staysActive: false, isActive: true)
    XCTAssertEqual(policy.enterBackground(staysActive: false, isActive: true), [])
    XCTAssertEqual(policy.enterBackground(staysActive: true, isActive: true), [])
  }

  func testForegroundEnablesVideoWhenItWasDisabled() {
    var policy = BackgroundPolicy()
    _ = policy.enterBackground(staysActive: true, isActive: true)
    XCTAssertEqual(policy.enterForeground(), [.enableVideo])
    XCTAssertFalse(policy.inBackground)
    XCTAssertFalse(policy.videoDisabled)
  }

  func testForegroundWithoutDisabledVideoOnlyLeavesBackground() {
    var policy = BackgroundPolicy()
    _ = policy.enterBackground(staysActive: false, isActive: true)
    XCTAssertEqual(policy.enterForeground(), [])
    XCTAssertFalse(policy.inBackground)
  }

  func testStaysActiveChangedOutsideBackgroundYieldsNothing() {
    var policy = BackgroundPolicy()
    XCTAssertEqual(policy.staysActiveChanged(staysActive: false, isActive: true), [])
    XCTAssertEqual(policy.staysActiveChanged(staysActive: true, isActive: true), [])
    XCTAssertFalse(policy.videoDisabled)
  }

  func testStaysActiveTurnedOffInBackgroundPausesWhenActiveAndKeepsVideoDisabled() {
    var policy = BackgroundPolicy()
    _ = policy.enterBackground(staysActive: true, isActive: true)
    XCTAssertEqual(policy.staysActiveChanged(staysActive: false, isActive: true), [.pause])
    XCTAssertTrue(policy.videoDisabled)
    XCTAssertEqual(policy.enterForeground(), [.enableVideo])
  }

  func testStaysActiveTurnedOffInBackgroundWhenInactiveYieldsNothing() {
    var policy = BackgroundPolicy()
    _ = policy.enterBackground(staysActive: false, isActive: false)
    XCTAssertEqual(policy.staysActiveChanged(staysActive: false, isActive: false), [])
  }

  func testStaysActiveTurnedOnInBackgroundDisablesVideo() {
    var policy = BackgroundPolicy()
    _ = policy.enterBackground(staysActive: false, isActive: false)
    XCTAssertEqual(policy.staysActiveChanged(staysActive: true, isActive: false), [.disableVideo])
    XCTAssertTrue(policy.videoDisabled)
  }

  func testStaysActiveTurnedOnInBackgroundWithVideoAlreadyDisabledYieldsNothing() {
    var policy = BackgroundPolicy()
    _ = policy.enterBackground(staysActive: true, isActive: true)
    XCTAssertEqual(policy.staysActiveChanged(staysActive: true, isActive: true), [])
    XCTAssertTrue(policy.videoDisabled)
  }
}

final class InterruptionPolicyTests: XCTestCase {
  func testInterruptionBeganPausesWhenActiveAndRemembers() {
    var policy = InterruptionPolicy()
    XCTAssertEqual(policy.began(isActive: true), .pause)
    XCTAssertTrue(policy.pausedByInterruption)
  }

  func testInterruptionBeganWhenInactiveDoesNothing() {
    var policy = InterruptionPolicy()
    XCTAssertNil(policy.began(isActive: false))
    XCTAssertFalse(policy.pausedByInterruption)
  }

  func testInterruptionEndedResumesWhenPausedByInterruptionAndShouldResume() {
    var policy = InterruptionPolicy()
    _ = policy.began(isActive: true)
    XCTAssertEqual(policy.ended(shouldResume: true), .resume)
    XCTAssertFalse(policy.pausedByInterruption)
  }

  func testInterruptionEndedWithoutShouldResumeClearsFlag() {
    var policy = InterruptionPolicy()
    _ = policy.began(isActive: true)
    XCTAssertNil(policy.ended(shouldResume: false))
    XCTAssertFalse(policy.pausedByInterruption)
  }

  func testInterruptionEndedWithoutPriorPauseDoesNothing() {
    var policy = InterruptionPolicy()
    _ = policy.began(isActive: false)
    XCTAssertNil(policy.ended(shouldResume: true))
  }

  func testPermanentLossPausesWhenActiveAndNeverResumes() {
    var policy = InterruptionPolicy()
    _ = policy.began(isActive: true)
    XCTAssertEqual(policy.lostPermanently(isActive: true), .pause)
    XCTAssertFalse(policy.pausedByInterruption)
    XCTAssertNil(policy.ended(shouldResume: true))
  }

  func testPermanentLossWhenInactiveClearsFlagWithoutPausing() {
    var policy = InterruptionPolicy()
    _ = policy.began(isActive: true)
    XCTAssertNil(policy.lostPermanently(isActive: false))
    XCTAssertFalse(policy.pausedByInterruption)
  }

  func testBecomingNoisyPausesWhenActiveAndClearsFlag() {
    var policy = InterruptionPolicy()
    _ = policy.began(isActive: true)
    XCTAssertEqual(policy.becameNoisy(isActive: true), .pause)
    XCTAssertFalse(policy.pausedByInterruption)
    XCTAssertNil(policy.ended(shouldResume: true))
  }

  func testBecomingNoisyWhenInactiveDoesNothing() {
    var policy = InterruptionPolicy()
    XCTAssertNil(policy.becameNoisy(isActive: false))
    XCTAssertFalse(policy.pausedByInterruption)
  }

  func testResetClearsPausedByInterruption() {
    var policy = InterruptionPolicy()
    _ = policy.began(isActive: true)
    policy.reset()
    XCTAssertFalse(policy.pausedByInterruption)
    XCTAssertNil(policy.ended(shouldResume: true))
  }
}

final class MediaSessionStateTests: XCTestCase {
  private func state(
    hasNowPlaying: Bool = true,
    hasSource: Bool = true,
    status: PlayerStatus = .playing,
    currentTimeMs: Int = 4000,
    durationMs: Int = 10_000,
    rate: Double = 1.5,
    isSeekable: Bool = true,
    isLive: Bool = false,
    hasNextListener: Bool = false,
    hasPreviousListener: Bool = false
  ) -> MediaSessionState {
    MediaSessionState.from(
      hasNowPlaying: hasNowPlaying,
      hasSource: hasSource,
      status: status,
      currentTimeMs: currentTimeMs,
      durationMs: durationMs,
      rate: rate,
      isSeekable: isSeekable,
      isLive: isLive,
      hasNextListener: hasNextListener,
      hasPreviousListener: hasPreviousListener
    )
  }

  func testSessionIsActiveOnlyWithNowPlayingAndSource() {
    XCTAssertTrue(state().isActive)
    XCTAssertFalse(state(hasNowPlaying: false).isActive)
    XCTAssertFalse(state(hasSource: false).isActive)
  }

  func testPlaybackMapsEveryStatus() {
    let expected: [PlayerStatus: MediaSessionPlayback] = [
      .idle: .none,
      .opening: .buffering,
      .buffering: .buffering,
      .playing: .playing,
      .paused: .paused,
      .stopped: .stopped,
      .ended: .stopped,
      .error: .error,
    ]
    expected.forEach { status, playback in
      XCTAssertEqual(state(status: status).playback, playback, "\(status)")
    }
  }

  func testPositionAndDurationPassThroughForSeekableMedia() {
    let result = state()
    XCTAssertEqual(result.positionMs, 4000)
    XCTAssertEqual(result.durationMs, 10_000)
  }

  func testPositionAndDurationAreZeroWhenLive() {
    let result = state(durationMs: 10_000, isLive: true)
    XCTAssertEqual(result.positionMs, 0)
    XCTAssertEqual(result.durationMs, 0)
  }

  func testUnknownDurationIsZero() {
    XCTAssertEqual(state(durationMs: -1).durationMs, 0)
  }

  func testPlaybackRateIsRateOnlyWhenPlaying() {
    XCTAssertEqual(state(status: .playing).playbackRate, 1.5)
    XCTAssertEqual(state(status: .paused).playbackRate, 0)
    XCTAssertEqual(state(status: .buffering).playbackRate, 0)
  }

  func testBaseCommandsAreAlwaysAvailable() {
    XCTAssertEqual(state(isSeekable: false).commands, [.play, .pause, .togglePlayPause, .stop])
  }

  func testSeekAndSkipCommandsNeedSeekableNonLiveMedia() {
    let seeking: Set<RemoteCommand> = [.seek, .skipForward, .skipBackward]
    XCTAssertTrue(seeking.isSubset(of: state(isSeekable: true, isLive: false).commands))
    XCTAssertTrue(seeking.isDisjoint(with: state(isSeekable: false, isLive: false).commands))
    XCTAssertTrue(seeking.isDisjoint(with: state(isSeekable: true, isLive: true).commands))
  }

  func testNextAndPreviousFollowListeners() {
    XCTAssertTrue(state(hasNextListener: true).commands.contains(.next))
    XCTAssertFalse(state(hasNextListener: true).commands.contains(.previous))
    XCTAssertTrue(state(hasPreviousListener: true).commands.contains(.previous))
    XCTAssertFalse(state(hasPreviousListener: true).commands.contains(.next))
  }
}

final class PlayerStatusActivityTests: XCTestCase {
  func testStatusIsActiveOnlyForOpeningBufferingAndPlaying() {
    let active = Set([PlayerStatus.idle, .opening, .buffering, .playing, .paused, .stopped, .ended, .error].filter(\.isActive))
    XCTAssertEqual(active, [.opening, .buffering, .playing])
  }
}

final class SkipIntervalContractTests: XCTestCase {
  func testRemoteSkipIntervalDefaultsAndIgnoresNaN() {
    XCTAssertEqual(PlayerSettings().remoteSkipInterval, 10_000)
    XCTAssertNil(VlcContract.clampSkipInterval(.nan))
  }

  func testClampSkipIntervalRoundsHalfUpAndClamps() {
    XCTAssertEqual(VlcContract.clampSkipInterval(10), 1000)
    XCTAssertEqual(VlcContract.clampSkipInterval(90_000), 60_000)
    XCTAssertEqual(VlcContract.clampSkipInterval(15_000.4), 15_000)
    XCTAssertEqual(VlcContract.clampSkipInterval(15_000.5), 15_001)
    XCTAssertEqual(VlcContract.clampSkipInterval(.infinity), 60_000)
    XCTAssertEqual(VlcContract.clampSkipInterval(-.infinity), 1000)
    XCTAssertNil(VlcContract.clampSkipInterval(.nan))
  }
}
