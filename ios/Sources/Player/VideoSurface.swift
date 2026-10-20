import UIKit

protocol VideoHost: AnyObject {
  var hostView: UIView { get }
  var hostResizeMode: ResizeMode { get }
}

final class VideoSurface {
  let container = VideoSurface.makeContainer()
  weak var engine: VlcEngine?
  private var hosts = SurfaceStack<AnyObject>()
  private var lastResize: ResizeSettings?

  var hasHosts: Bool {
    !hosts.isEmpty
  }

  private var topHost: VideoHost? {
    hosts.top as? VideoHost
  }

  func attach(_ host: VideoHost) {
    hosts.attach(host)
    mount()
  }

  func detach(_ host: VideoHost) {
    hosts.detach(host)
  }

  func hostDidLayout(_ host: VideoHost) {
    guard topHost === host else { return }
    mount()
  }

  func removeAll() {
    hosts.removeAll()
    container.removeFromSuperview()
  }

  func mount() {
    guard let host = topHost else {
      container.removeFromSuperview()
      return
    }
    let isNewHost = container.superview !== host.hostView
    if isNewHost {
      host.hostView.insertSubview(container, at: 0)
    }
    let sizeChanged = container.frame != host.hostView.bounds
    container.frame = host.hostView.bounds
    if isNewHost || sizeChanged {
      relayoutRenderedVideo()
    }
    applyResize(force: isNewHost || sizeChanged)
  }

  func applyResize(force: Bool) {
    guard let engine, let host = topHost else { return }
    let settings = ResizeGeometry.settings(mode: host.hostResizeMode, viewSize: host.hostView.bounds.size)
    guard force || settings != lastResize else { return }
    lastResize = settings
    engine.apply(settings)
  }

  private func relayoutRenderedVideo() {
    container.subviews.forEach { renderView in
      renderView.frame = container.bounds
      renderView.setNeedsLayout()
    }
    container.layoutIfNeeded()
  }

  private static func makeContainer() -> UIView {
    let container = UIView()
    container.backgroundColor = .black
    container.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    container.isUserInteractionEnabled = false
    return container
  }
}
