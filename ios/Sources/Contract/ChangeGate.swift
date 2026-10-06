import Foundation

struct ChangeGate<Value: Equatable> {
  private var lastEmitted: Value?

  init(initial: Value? = nil) {
    lastEmitted = initial
  }

  mutating func shouldEmit(_ value: Value) -> Bool {
    guard value != lastEmitted else { return false }
    lastEmitted = value
    return true
  }

  mutating func reset(to value: Value? = nil) {
    lastEmitted = value
  }
}

struct VideoDimensions: Equatable {
  static let zero = VideoDimensions(width: 0, height: 0)

  let width: Int
  let height: Int

  var isRenderable: Bool { width > 0 && height > 0 }
}

struct VideoSizeGate {
  private var gate = ChangeGate<VideoDimensions>()

  mutating func shouldEmit(_ size: VideoDimensions) -> Bool {
    size.isRenderable && gate.shouldEmit(size)
  }

  mutating func reset() {
    gate.reset()
  }
}

struct VolumeState: Equatable {
  let volume: Int
  let muted: Bool
}
