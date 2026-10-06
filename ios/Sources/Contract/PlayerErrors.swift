import Foundation

struct PlayerFailure: Equatable {
  let code: PlayerErrorCode
  let message: String
}

enum PlayerErrors {
  static let invalidSourceMessage = "Invalid source uri"
  static let networkMessage = "Could not open the media over the network"
  static let mediaMessage = "The media could not be played"
  static let addSubtitleWithoutSource = "addSubtitle requires a source"
  static let noVideoFrame = "No video frame available"

  static let invalidSource = PlayerFailure(code: .invalidSource, message: invalidSourceMessage)
  static let network = PlayerFailure(code: .network, message: networkMessage)
  static let media = PlayerFailure(code: .media, message: mediaMessage)

  static func engineFailure(uri: String, hasReachedPlaying: Bool) -> PlayerFailure {
    !hasReachedPlaying && MediaUri.isNetwork(uri) ? network : media
  }
}
