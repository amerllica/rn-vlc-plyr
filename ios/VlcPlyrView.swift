import UIKit
import MobileVLCKit

@objc(VlcPlyrView)
final class VlcPlyrView: UIView {

    private var player: VLCMediaPlayer?
    private var media: VLCMedia?

    @objc var url: String? {
        didSet {
            if oldValue != url {
                playVideo(urlString: url)
            }
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
    }

    private func playVideo(urlString: String?) {
        guard let urlString = urlString, let videoURL = URL(string: urlString) else {
            player?.stop()
            return
        }

        media = VLCMedia(url: videoURL)
        player?.media = media
        player?.play()
    }

    @objc func play() {
        if let player = player, !player.isPlaying {
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
