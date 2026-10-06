import NitroModules
import UIKit

final class HybridVlcPlayerView: HybridVlcPlayerViewSpec, VideoHost {
  private let hostingView = VideoHostView()
  private weak var attachedPlayer: HybridVlcPlayer?

  var view: UIView { hostingView }
  var hostView: UIView { hostingView }
  var hostResizeMode: ResizeMode {
    switch resizeMode ?? .contain {
    case .contain: return .contain
    case .cover: return .cover
    case .stretch: return .stretch
    case .original: return .original
    }
  }

  var player: (any HybridVlcPlayerSpec)? {
    didSet { attach(to: player as? HybridVlcPlayer) }
  }

  var resizeMode: VlcResizeMode? {
    didSet { attachedPlayer?.hostDidLayout(self) }
  }

  var surfaceType: VlcSurfaceType?

  override init() {
    super.init()
    hostingView.onLayout = { [weak self] in
      guard let self else { return }
      self.attachedPlayer?.hostDidLayout(self)
    }
  }

  func onDropView() {
    attach(to: nil)
  }

  private func attach(to next: HybridVlcPlayer?) {
    guard next !== attachedPlayer else { return }
    attachedPlayer?.detach(self)
    attachedPlayer = next
    next?.attach(self)
  }
}
