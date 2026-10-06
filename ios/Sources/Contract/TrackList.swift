import Foundation

struct TrackInfo: Equatable {
  let id: Int
  let name: String
}

enum TrackList {
  static let disabledTrackId = -1

  static func build(ids: [Int], names: [String]) -> [TrackInfo] {
    ids.enumerated().compactMap { index, id in
      guard id != disabledTrackId else { return nil }
      let name = index < names.count ? names[index] : ""
      return TrackInfo(id: id, name: name.isEmpty ? "Track \(id)" : name)
    }
  }

  static func canSelect(_ id: Int, in tracks: [TrackInfo]) -> Bool {
    id == disabledTrackId || tracks.contains { $0.id == id }
  }
}

struct TracksSnapshot: Equatable {
  var audioTracks: [TrackInfo] = []
  var subtitleTracks: [TrackInfo] = []
  var selectedAudioTrack = TrackList.disabledTrackId
  var selectedSubtitleTrack = TrackList.disabledTrackId
}
