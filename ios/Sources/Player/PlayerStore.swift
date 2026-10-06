import Foundation

struct PlayerMirror {
  var source: VlcSource?
  var settings = PlayerSettings()
  var media = MediaState()
  var isReleased = false
}

final class PlayerEvents {
  let statusChange = ListenerRegistry<VlcStatus>()
  let timeUpdate = ListenerRegistry<VlcTimeUpdate>()
  let loaded = ListenerRegistry<VlcLoadedInfo>()
  let ended = ListenerRegistry<Void>()
  let error = ListenerRegistry<VlcError>()
  let volumeChange = ListenerRegistry<VlcVolumeInfo>()
  let tracksChange = ListenerRegistry<VlcTracksInfo>()
  let videoSizeChange = ListenerRegistry<VlcVideoSize>()

  func removeAll() {
    statusChange.removeAll()
    timeUpdate.removeAll()
    loaded.removeAll()
    ended.removeAll()
    error.removeAll()
    volumeChange.removeAll()
    tracksChange.removeAll()
    videoSizeChange.removeAll()
  }
}

final class PlayerStore {
  let events = PlayerEvents()
  private let mirror = Locked(PlayerMirror())

  var isReleased: Bool { mirror.read { $0.isReleased } }
  var source: VlcSource? { mirror.read { $0.source } }
  var settings: PlayerSettings { mirror.read { $0.settings } }
  var media: MediaState { mirror.read { $0.media } }

  func read<Result>(_ body: (PlayerMirror) -> Result) -> Result {
    mirror.read(body)
  }

  func setSource(_ source: VlcSource?) -> Bool {
    mirror.mutate { state in
      guard !state.isReleased else { return false }
      state.source = source
      return true
    }
  }

  func updateSettings(_ body: (inout PlayerSettings) -> Void) -> (before: PlayerSettings, after: PlayerSettings)? {
    mirror.mutate { state in
      guard !state.isReleased else { return nil }
      let before = state.settings
      body(&state.settings)
      return (before, state.settings)
    }
  }

  func updateMedia(_ body: (inout MediaState) -> Void) {
    mirror.mutate { state in
      guard !state.isReleased else { return }
      body(&state.media)
    }
  }

  func markReleased() -> Bool {
    let isFirstRelease = mirror.mutate { state -> Bool in
      guard !state.isReleased else { return false }
      state = PlayerMirror()
      state.isReleased = true
      return true
    }
    if isFirstRelease {
      events.removeAll()
    }
    return isFirstRelease
  }

  func emit<Payload>(_ registry: KeyPath<PlayerEvents, ListenerRegistry<Payload>>, _ payload: Payload) {
    guard !isReleased else { return }
    events[keyPath: registry].emit(payload)
  }
}
