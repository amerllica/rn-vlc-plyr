import UIKit

final class VideoHostView: UIView {
  var onLayout: (() -> Void)?

  override init(frame: CGRect) {
    super.init(frame: frame)
    backgroundColor = .black
    clipsToBounds = true
  }

  required init?(coder: NSCoder) {
    super.init(coder: coder)
    backgroundColor = .black
    clipsToBounds = true
  }

  override func layoutSubviews() {
    super.layoutSubviews()
    onLayout?()
  }
}
