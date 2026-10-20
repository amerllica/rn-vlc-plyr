import Foundation

protocol MediaSessionControl: AnyObject {
  func play()
  func pause()
  func seek(toMs target: Double)
  func seek(byMs delta: Double)
  func pauseForSession()
  func resumeForSession()
  func setVideoTrackEnabled(_ enabled: Bool)
}

final class MediaSessionCoordinator {
  private let store: PlayerStore
  private weak var control: MediaSessionControl?
  private var background = BackgroundPolicy()
  private var interruption = InterruptionPolicy()
  private var staysActiveInBackground = false
  private var lastNowPlaying: NowPlayingInfo?
  private var observers: [NotificationObservers] = []
  private var isShutDown = false

  init(store: PlayerStore, control: MediaSessionControl) {
    self.store = store
    self.control = control
    observers = [
      AppLifecycleObserver.observe(
        onBackground: { [weak self] in self?.didEnterBackground() },
        onForeground: { [weak self] in self?.willEnterForeground() }
      ),
      AudioSessionObserver.observe { [weak self] in self?.handle($0) },
    ]
    refreshWhenPresenceChanges(store.events.remoteNext)
    refreshWhenPresenceChanges(store.events.remotePrevious)
  }

  var isVideoDisabled: Bool {
    background.videoDisabled
  }

  var snapshot: MediaSessionSnapshot {
    let events = store.events
    let (hasSource, settings, media) = store.read { ($0.source != nil, $0.settings, $0.media) }
    let state = MediaSessionState.from(
      hasNowPlaying: settings.nowPlaying != nil,
      hasSource: hasSource,
      status: media.status,
      currentTimeMs: media.currentTime,
      durationMs: media.duration,
      rate: settings.rate,
      isSeekable: media.isSeekable,
      isLive: media.isLive,
      hasNextListener: events.remoteNext.hasListeners,
      hasPreviousListener: events.remotePrevious.hasListeners
    )
    return MediaSessionSnapshot(state: state, info: settings.nowPlaying, isLive: media.isLive, defaultRate: settings.rate, skipIntervalMs: settings.remoteSkipInterval)
  }

  func userDidCommand() {
    interruption.reset()
  }

  func sourceDidChange() {
    interruption.reset()
    refresh()
  }

  func settingsDidChange() {
    resetArtworkIfNowPlayingChanged()
    let staysActive = store.settings.staysActiveInBackground
    if staysActive != staysActiveInBackground {
      staysActiveInBackground = staysActive
      if staysActive {
        BackgroundAudioSetup.warnOnceIfMissing()
      }
      apply(background.staysActiveChanged(staysActive: staysActive, isActive: isPlaybackActive))
    }
    refresh()
  }

  func refresh() {
    guard !isShutDown else { return }
    MediaSessionOwner.shared.update(self)
  }

  func shutdown() {
    guard !isShutDown else { return }
    isShutDown = true
    observers.removeAll()
    MediaSessionOwner.shared.resign(self)
  }

  func handle(_ request: RemoteRequest) {
    guard let control else { return }
    let skipInterval = Double(store.settings.remoteSkipInterval)
    switch request {
    case .play: control.play()
    case .pause: control.pause()
    case .togglePlayPause: togglePlayback(control)
    case .seek(let milliseconds): control.seek(toMs: milliseconds)
    case .skipForward: control.seek(byMs: skipInterval)
    case .skipBackward: control.seek(byMs: -skipInterval)
    case .next: store.emit(\.remoteNext, ())
    case .previous: store.emit(\.remotePrevious, ())
    }
  }

  private func resetArtworkIfNowPlayingChanged() {
    let nowPlaying = store.settings.nowPlaying
    guard nowPlaying != lastNowPlaying else { return }
    lastNowPlaying = nowPlaying
    if let uri = nowPlaying?.artworkUri {
      MediaSessionOwner.shared.resetArtwork(uri)
    }
  }

  private var isPlaybackActive: Bool {
    store.media.status.isActive
  }

  private func togglePlayback(_ control: MediaSessionControl) {
    let status = store.media.status
    if status == .playing || status == .buffering {
      control.pause()
    } else {
      control.play()
    }
  }

  private func didEnterBackground() {
    apply(background.enterBackground(staysActive: store.settings.staysActiveInBackground, isActive: isPlaybackActive))
  }

  private func willEnterForeground() {
    apply(background.enterForeground())
  }

  private func handle(_ event: AudioSessionEvent) {
    switch event {
    case .interruptionBegan:
      apply(interruption.began(isActive: isPlaybackActive))
    case .interruptionEnded(let shouldResume):
      apply(interruption.ended(shouldResume: shouldResume))
    case .oldDeviceUnavailable:
      apply(interruption.becameNoisy(isActive: isPlaybackActive))
    }
  }

  private func apply(_ effects: [BackgroundEffect]) {
    for effect in effects {
      switch effect {
      case .pause:
        interruption.reset()
        control?.pauseForSession()
      case .disableVideo:
        control?.setVideoTrackEnabled(false)
      case .enableVideo:
        control?.setVideoTrackEnabled(true)
      }
    }
  }

  private func apply(_ effect: InterruptionEffect?) {
    switch effect {
    case .pause: control?.pauseForSession()
    case .resume: control?.resumeForSession()
    case nil: return
    }
  }

  private func refreshWhenPresenceChanges(_ registry: ListenerRegistry<Void>) {
    registry.observePresence { [weak self] _ in
      MainThread.runNowOrAsync { self?.refresh() }
    }
  }
}
