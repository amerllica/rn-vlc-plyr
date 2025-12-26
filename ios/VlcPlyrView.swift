import UIKit
import MobileVLCKit

@objc(VlcPlyrView)
final class VlcPlyrView: UIView, VLCMediaPlayerDelegate {
  
  private var player: VLCMediaPlayer?
  private var media: VLCMedia?
  
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
  }
  
  private func prepareMedia(startingTime: Double? = nil) {
    guard let urlString = url,
          let videoURL = URL(string: urlString) else {
      player?.stop()
      return
    }
    
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
  
  @MainActor
  @objc func cleanup() {
    player?.stop()
    player = nil
    media = nil
  }
  
  deinit {
    DispatchQueue.main.async {
      self.cleanup()
    }
  }
}
