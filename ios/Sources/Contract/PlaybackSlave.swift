import Foundation

struct PlaybackSlave: Equatable {
  enum Kind: Hashable {
    case audio
    case subtitle
  }

  let kind: Kind
  let url: URL
  let select: Bool
}
