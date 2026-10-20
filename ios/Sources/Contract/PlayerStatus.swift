import Foundation

enum PlayerStatus: String {
  case idle
  case opening
  case buffering
  case playing
  case paused
  case stopped
  case ended
  case error

  var isActive: Bool {
    self == .opening || self == .buffering || self == .playing
  }
}

enum PlayerErrorCode: String {
  case invalidSource
  case network
  case media
  case unknown
}
