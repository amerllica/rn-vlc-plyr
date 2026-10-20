import MediaPlayer

enum RemoteRequest: Equatable {
  case play
  case pause
  case togglePlayPause
  case seek(toMs: Double)
  case skipForward
  case skipBackward
  case next
  case previous
}

final class RemoteCommandRouter {
  private static let millisecondsPerSecond = 1000.0

  private let route: (RemoteRequest) -> Bool
  private var registrations: [(command: MPRemoteCommand, target: Any)] = []

  init(route: @escaping (RemoteRequest) -> Bool) {
    self.route = route
  }

  private var center: MPRemoteCommandCenter { .shared() }

  private var bindings: [(RemoteCommand, MPRemoteCommand)] {
    [
      (.play, center.playCommand),
      (.pause, center.pauseCommand),
      (.togglePlayPause, center.togglePlayPauseCommand),
      (.seek, center.changePlaybackPositionCommand),
      (.skipForward, center.skipForwardCommand),
      (.skipBackward, center.skipBackwardCommand),
      (.next, center.nextTrackCommand),
      (.previous, center.previousTrackCommand),
    ]
  }

  func install() {
    guard registrations.isEmpty else { return }
    register(center.playCommand) { _ in .play }
    register(center.pauseCommand) { _ in .pause }
    register(center.togglePlayPauseCommand) { _ in .togglePlayPause }
    register(center.changePlaybackPositionCommand) { event in
      (event as? MPChangePlaybackPositionCommandEvent).map { .seek(toMs: $0.positionTime * Self.millisecondsPerSecond) }
    }
    register(center.skipForwardCommand) { _ in .skipForward }
    register(center.skipBackwardCommand) { _ in .skipBackward }
    register(center.nextTrackCommand) { _ in .next }
    register(center.previousTrackCommand) { _ in .previous }
  }

  func uninstall() {
    registrations.forEach { $0.command.removeTarget($0.target) }
    registrations.removeAll()
    bindings.forEach { $0.1.isEnabled = false }
  }

  func apply(commands: Set<RemoteCommand>, skipIntervalMs: Int) {
    bindings.forEach { command, remote in
      remote.isEnabled = commands.contains(command)
    }
    let interval = [NSNumber(value: Double(skipIntervalMs) / Self.millisecondsPerSecond)]
    center.skipForwardCommand.preferredIntervals = interval
    center.skipBackwardCommand.preferredIntervals = interval
  }

  private func register(_ command: MPRemoteCommand, _ request: @escaping (MPRemoteCommandEvent) -> RemoteRequest?) {
    let target = command.addTarget { [weak self] event in
      guard let self, let request = request(event) else { return .commandFailed }
      return self.route(request) ? .success : .noActionableNowPlayingItem
    }
    registrations.append((command, target))
  }
}
