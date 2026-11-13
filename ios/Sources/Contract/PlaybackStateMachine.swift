import Foundation

enum EngineState: Equatable {
  case stopped
  case opening
  case buffering
  case ended
  case error
  case playing
  case paused
  case esAdded
}

enum PlaybackEffect: Equatable {
  case startedPlaying
  case status(PlayerStatus)
  case loaded
  case ended
  case error(PlayerFailure)
  case restartForLoop
}

enum PlayAction: Equatable {
  case none
  case start
  case resume
  case restart
  case reload
}

enum ReloadMode: Equatable {
  case none
  case inPlace
  case withoutPlaying
}

enum SourceState: Equatable {
  case none
  case invalid
  case valid(uri: String)
}

enum SeekAction: Equatable {
  case none
  case seek(Int)
  case restartThenSeek(Int)
}

struct PlaybackStateMachine {
  private(set) var status: PlayerStatus = .idle
  private(set) var hasReachedPlaying = false
  private var source = SourceState.none
  private var loadedEmitted = false
  private var expectingPlayback = false
  private var restartingForLoop = false

  mutating func load(uri: String, autoPlay: Bool) -> [PlaybackEffect] {
    resetMediaFlags()
    source = .valid(uri: uri)
    return autoPlay ? beginPlayback() : transition(to: .idle)
  }

  mutating func clear() -> [PlaybackEffect] {
    resetMediaFlags()
    source = .none
    return transition(to: .idle)
  }

  mutating func failInvalidSource() -> [PlaybackEffect] {
    resetMediaFlags()
    source = .invalid
    guard status != .error else { return [] }
    return fail(with: PlayerErrors.invalidSource)
  }

  var hasSource: Bool {
    source != .none
  }

  var reloadMode: ReloadMode {
    if !hasSource || status == .error { return .none }
    return isMediaActive ? .inPlace : .withoutPlaying
  }

  var isRestartingForLoop: Bool {
    restartingForLoop
  }

  mutating func beginPlayback() -> [PlaybackEffect] {
    expectingPlayback = true
    restartingForLoop = false
    return transition(to: .opening)
  }

  mutating func reloadForRetry() -> [PlaybackEffect] {
    resetMediaFlags()
    return beginPlayback()
  }

  mutating func requestStop() -> [PlaybackEffect] {
    expectingPlayback = false
    restartingForLoop = false
    return transition(to: .stopped)
  }

  func playAction() -> PlayAction {
    switch (source, status) {
    case (.none, _): return .none
    case (.invalid, _): return .reload
    case (.valid, .idle): return .start
    case (.valid, .paused): return .resume
    case (.valid, .ended), (.valid, .stopped): return .restart
    case (.valid, .error): return .reload
    case (.valid, .opening), (.valid, .buffering), (.valid, .playing): return .none
    }
  }

  var isMediaActive: Bool {
    status == .opening || status == .buffering || status == .playing || status == .paused
  }

  var canPause: Bool {
    status == .playing || status == .buffering
  }

  var canStop: Bool {
    status != .idle
  }

  func seekAction(targetMs: Double, duration: Int, isSeekable: Bool) -> SeekAction {
    guard hasSource, isSeekable,
          let target = VlcContract.clampSeekTarget(targetMs, duration: duration) else { return .none }
    switch status {
    case .ended, .stopped: return .restartThenSeek(target)
    default: return .seek(target)
    }
  }

  mutating func handle(engine state: EngineState, engineIsPlaying: Bool, loop: Bool) -> [PlaybackEffect] {
    guard expectingPlayback else { return [] }
    switch state {
    case .opening:
      return restartingForLoop ? [] : transition(to: .opening)
    case .buffering:
      return ignoresBuffering(engineIsPlaying: engineIsPlaying) ? [] : transition(to: .buffering)
    case .playing:
      return enterPlaying()
    case .paused:
      return restartingForLoop ? [] : transition(to: .paused)
    case .stopped:
      return []
    case .ended:
      return restartingForLoop ? [] : reachEnd(loop: loop)
    case .error:
      return fail(with: PlayerErrors.engineFailure(uri: validUri, hasReachedPlaying: hasReachedPlaying))
    case .esAdded:
      return []
    }
  }

  private var validUri: String {
    guard case .valid(let uri) = source else { return "" }
    return uri
  }

  private func ignoresBuffering(engineIsPlaying: Bool) -> Bool {
    restartingForLoop || engineIsPlaying || status == .playing || status == .paused
  }

  private mutating func enterPlaying() -> [PlaybackEffect] {
    let endsLoopRestart = restartingForLoop
    restartingForLoop = false
    hasReachedPlaying = true
    let statusChange = transition(to: .playing)
    guard endsLoopRestart || !statusChange.isEmpty else { return [] }
    var effects = [PlaybackEffect.startedPlaying] + statusChange
    if !loadedEmitted {
      loadedEmitted = true
      effects.append(.loaded)
    }
    return effects
  }

  private mutating func reachEnd(loop: Bool) -> [PlaybackEffect] {
    if loop {
      restartingForLoop = true
      return [.restartForLoop]
    }
    expectingPlayback = false
    return transition(to: .ended) + [.ended]
  }

  private mutating func fail(with error: PlayerFailure) -> [PlaybackEffect] {
    expectingPlayback = false
    restartingForLoop = false
    return transition(to: .error) + [.error(error)]
  }

  private mutating func transition(to next: PlayerStatus) -> [PlaybackEffect] {
    guard next != status else { return [] }
    status = next
    return [.status(next)]
  }

  private mutating func resetMediaFlags() {
    hasReachedPlaying = false
    loadedEmitted = false
    expectingPlayback = false
    restartingForLoop = false
  }
}
