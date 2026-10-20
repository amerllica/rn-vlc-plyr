import Foundation

enum RemoteCommand: CaseIterable {
  case play
  case pause
  case togglePlayPause
  case stop
  case seek
  case skipForward
  case skipBackward
  case next
  case previous
}

enum MediaSessionPlayback: Equatable {
  case none
  case buffering
  case playing
  case paused
  case stopped
  case error
}

struct MediaSessionState: Equatable {
  static let alwaysAvailableCommands: Set<RemoteCommand> = [.play, .pause, .togglePlayPause, .stop]
  static let seekingCommands: Set<RemoteCommand> = [.seek, .skipForward, .skipBackward]

  let isActive: Bool
  let playback: MediaSessionPlayback
  let positionMs: Int
  let durationMs: Int
  let playbackRate: Double
  let commands: Set<RemoteCommand>

  static func from(
    hasNowPlaying: Bool,
    hasSource: Bool,
    status: PlayerStatus,
    currentTimeMs: Int,
    durationMs: Int,
    rate: Double,
    isSeekable: Bool,
    isLive: Bool,
    hasNextListener: Bool,
    hasPreviousListener: Bool
  ) -> MediaSessionState {
    let playback = playback(for: status)
    return MediaSessionState(
      isActive: hasNowPlaying && hasSource,
      playback: playback,
      positionMs: isLive ? 0 : currentTimeMs,
      durationMs: isLive ? 0 : max(durationMs, 0),
      playbackRate: playback == .playing ? rate : 0,
      commands: commands(canSeek: isSeekable && !isLive, hasNextListener: hasNextListener, hasPreviousListener: hasPreviousListener)
    )
  }

  private static func playback(for status: PlayerStatus) -> MediaSessionPlayback {
    switch status {
    case .idle: return .none
    case .opening, .buffering: return .buffering
    case .playing: return .playing
    case .paused: return .paused
    case .stopped, .ended: return .stopped
    case .error: return .error
    }
  }

  private static func commands(canSeek: Bool, hasNextListener: Bool, hasPreviousListener: Bool) -> Set<RemoteCommand> {
    var commands = alwaysAvailableCommands
    if canSeek {
      commands.formUnion(seekingCommands)
    }
    if hasNextListener {
      commands.insert(.next)
    }
    if hasPreviousListener {
      commands.insert(.previous)
    }
    return commands
  }
}
