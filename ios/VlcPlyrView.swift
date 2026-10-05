import UIKit
import MediaPlayer
import MobileVLCKit

@objc protocol VlcPlyrViewDelegate: AnyObject {
  func vlcPlyrView(_ view: VlcPlyrView, didEncounterError message: String, code: Int)
  @objc optional func vlcPlyrView(_ view: VlcPlyrView, didChangeVolume volume: Double)
}

@objc(VlcPlyrView)
final class VlcPlyrView: UIView, VLCMediaPlayerDelegate {

  private var player: VLCMediaPlayer?
  private var media: VLCMedia?
  private var hasReportedError: Bool = false
  private var mpVolumeView: MPVolumeView?
  private var volumeSlider: UISlider?
  @objc weak var errorDelegate: VlcPlyrViewDelegate?

  @objc var url: String? {
    didSet {
      if oldValue != url {
        prepareMedia()
      }
    }
  }

  @objc var autoPlay: Bool = true {
    didSet {
      guard let player = player else { return }

      if autoPlay {
        if player.media != nil && !player.isPlaying {
          player.play()
        }
      } else {
        if player.isPlaying {
          player.pause()
        }
      }
    }
  }

  @objc var loop: Bool = false {
    didSet {
      guard oldValue != loop else { return }

      let currentTime = player?.time.intValue ?? 0 // Milliseconds

      prepareMedia(startingTime: Double(currentTime))
    }
  }

  @objc var muted: Bool = false {
    didSet {
      player?.audio?.isMuted = muted
    }
  }

  override init(frame: CGRect) {
    super.init(frame: frame)
    setupVLCPlayer()
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  private func setupVLCPlayer() {
    self.backgroundColor = .black

    player = VLCMediaPlayer()
    player?.drawable = self
    player?.delegate = self

    setupVolumeObserver()
  }

  private func setupVolumeObserver() {
    // AVAudioSession KVO cannot be used here: MobileVLCKit calls setActive(false) on
    // the shared (process-wide) session when playback stops, which kills all KVO
    // notifications regardless of what category we configure.
    //
    // MPVolumeView's internal UISlider fires valueChanged directly from the hardware
    // volume buttons, completely independent of AVAudioSession state. This is the
    // same mechanism used by react-native-video and other iOS media libraries.
    let volumeView = MPVolumeView(frame: CGRect(x: -1000, y: -1000, width: 1, height: 1))
    volumeView.showsVolumeSlider = true
    addSubview(volumeView)
    mpVolumeView = volumeView

    volumeSlider = volumeView.subviews.compactMap { $0 as? UISlider }.first
    volumeSlider?.addTarget(self, action: #selector(volumeSliderChanged(_:)), for: .valueChanged)
  }

  @objc private func volumeSliderChanged(_ slider: UISlider) {
    let volumePercent = Double(slider.value) * 100.0
    errorDelegate?.vlcPlyrView?(self, didChangeVolume: volumePercent)
  }

  private func prepareMedia(startingTime: Double? = nil) {
    guard let urlString = url,
          !urlString.isEmpty else {
      player?.stop()
      emitError("Invalid or missing URL", code: -4)
      return
    }

    guard let videoURL = URL(string: urlString) else {
      player?.stop()
      emitError("Malformed URL: \(urlString)", code: -5)
      return
    }

    hasReportedError = false
    let wasPlaying = player?.isPlaying ?? false

    media = VLCMedia(url: videoURL)

    if loop {
      media?.addOption(":input-repeat=65535")
    } else {
      media?.addOption(":input-repeat=0")
    }

    player?.media = media

    if autoPlay || wasPlaying {
      player?.play()
    }

    if let time = startingTime, time > 0 {
      DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
        self.player?.time = VLCTime(int: Int32(time))
      }
    }
  }

  private func emitError(_ message: String, code: Int) {
    guard !hasReportedError else { return }
    hasReportedError = true

    DispatchQueue.main.async { [weak self] in
      guard let self = self else { return }

      self.errorDelegate?.vlcPlyrView(self, didEncounterError: message, code: code)
    }
  }

  @objc func testError() {
    emitError("Test error from Swift", code: -999)
  }

  @objc func play() {
    guard let player = player else {
      return
    }

    if player.state == .stopped {
      player.stop()
    }

    if !player.isPlaying {
      player.play()
    }
  }

  @objc func pause() {
    if let player = player, player.isPlaying {
      player.pause()
    }
  }

  @objc func stop() {
    player?.stop()
  }

  @objc func seek(_ timeMs: Double) {
    guard let player = player,
          timeMs.isFinite,
          timeMs >= 0 else {
      return
    }

    DispatchQueue.main.async {
      player.time = VLCTime(int: Int32(timeMs))
    }
  }

  @objc func setVolume(_ volume: Double) {
    guard let player = player,
          volume.isFinite else {
      return
    }

    let safeVolume = max(0, min(Int32(volume), 100))

    DispatchQueue.main.async {
      player.audio?.volume = safeVolume
    }
  }

  nonisolated func mediaPlayerStateChanged(_ aNotification: Notification!) {
    Task { @MainActor in
      guard let player = player else { return }

      if player.state == .error {
        emitError("VLC Player encountered an error", code: -1)
      }
    }
  }

  nonisolated func mediaPlayerDidFinishPlaying(_ aNotification: Notification!) {
    let stopReason = aNotification.userInfo?["VLCMediaPlayerStopReason"] as? Int

    Task { @MainActor in
      if stopReason == 2 {
        emitError("Media playback failed or encountered an error", code: -2)
      }
    }
  }

  nonisolated func mediaPlayerEncounteredError(_ aNotification: Notification!) {
    Task { @MainActor in
      emitError("VLC encountered an error during playback", code: -3)
    }
  }

  @MainActor
  @objc func cleanup() {
    volumeSlider?.removeTarget(self, action: #selector(volumeSliderChanged(_:)), for: .valueChanged)
    volumeSlider = nil
    mpVolumeView?.removeFromSuperview()
    mpVolumeView = nil
    player?.stop()
    player = nil
    media = nil
    hasReportedError = false
  }

  deinit {
    DispatchQueue.main.async {
      self.cleanup()
    }
  }
}
