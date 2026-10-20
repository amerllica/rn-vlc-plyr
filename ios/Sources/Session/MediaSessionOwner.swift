import UIKit

struct MediaSessionSnapshot {
  let state: MediaSessionState
  let info: NowPlayingInfo?
  let isLive: Bool
  let defaultRate: Double
  let skipIntervalMs: Int
}

final class MediaSessionOwner {
  static let shared = MediaSessionOwner()

  private weak var owner: MediaSessionCoordinator?
  private let nowPlayingCenter = NowPlayingCenter()
  private lazy var router = RemoteCommandRouter { [weak self] request in
    self?.route(request) ?? false
  }
  private lazy var artwork = ArtworkLoader { [weak self] in
    self?.refreshOwner()
  }

  func update(_ candidate: MediaSessionCoordinator) {
    let snapshot = candidate.snapshot
    guard snapshot.state.isActive else {
      resign(candidate)
      return
    }
    if snapshot.state.playback == .playing {
      owner = candidate
    }
    guard owner === candidate else { return }
    publish(snapshot)
  }

  func resign(_ candidate: MediaSessionCoordinator) {
    guard owner === candidate else { return }
    owner = nil
    router.uninstall()
    nowPlayingCenter.clear()
  }

  @discardableResult
  func route(_ request: RemoteRequest) -> Bool {
    guard let owner else { return false }
    owner.handle(request)
    return true
  }

  private func publish(_ snapshot: MediaSessionSnapshot) {
    router.install()
    router.apply(commands: snapshot.state.commands, skipIntervalMs: snapshot.skipIntervalMs)
    nowPlayingCenter.apply(snapshot, artwork: snapshot.info?.artworkUri.flatMap(artwork.image(for:)))
  }

  private func refreshOwner() {
    guard let owner else { return }
    update(owner)
  }
}
