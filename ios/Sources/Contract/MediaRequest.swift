import Foundation

struct MediaRequest: Equatable {
  static let addedAudioReaderOption = ":demux=avformat"

  let url: URL
  let options: [String]

  func readingAddedAudio(_ hasAddedAudio: Bool) -> MediaRequest {
    guard hasAddedAudio, !options.contains(Self.addedAudioReaderOption) else { return self }
    return MediaRequest(url: url, options: options + [Self.addedAudioReaderOption])
  }

  static func make(uri: String, userAgent: String? = nil, referrer: String? = nil, vlcOptions: [String]? = nil) -> MediaRequest? {
    guard let url = MediaUri.resolve(uri) else { return nil }
    return MediaRequest(url: url, options: buildOptions(userAgent: userAgent, referrer: referrer, vlcOptions: vlcOptions))
  }

  static func buildOptions(userAgent: String?, referrer: String?, vlcOptions: [String]?) -> [String] {
    var options: [String] = []
    if let userAgent { options.append(":http-user-agent=\(withoutLineBreaks(userAgent))") }
    if let referrer { options.append(":http-referrer=\(withoutLineBreaks(referrer))") }
    options.append(contentsOf: vlcOptions ?? [])
    return options
  }

  private static func withoutLineBreaks(_ value: String) -> String {
    String(value.unicodeScalars.filter { $0 != "\r" && $0 != "\n" })
  }
}

enum MediaUri {
  static let networkSchemes: Set<String> = ["http", "https", "rtsp", "rtmp", "rtp", "udp", "mms", "ftp", "smb"]

  private static let uriCharacters = CharacterSet.urlQueryAllowed.union(CharacterSet(charactersIn: "#%[]"))
  private static let userInfoPattern = "^([A-Za-z][A-Za-z0-9+.-]*://)[^/]*@"
  private static let schemePattern = try? NSRegularExpression(pattern: "^[A-Za-z][A-Za-z0-9+.-]*:")

  static func resolve(_ uri: String) -> URL? {
    if uri.hasPrefix("/") {
      return URL(fileURLWithPath: uri)
    }
    guard scheme(of: uri) != nil else { return nil }
    return URL(string: uri) ?? uri.addingPercentEncoding(withAllowedCharacters: uriCharacters).flatMap(URL.init(string:))
  }

  static func redacted(_ uri: String) -> String {
    String(
      uri.replacingOccurrences(of: userInfoPattern, with: "$1", options: .regularExpression)
        .prefix { $0 != "?" && $0 != "#" }
    )
  }

  static func scheme(of uri: String) -> String? {
    let fullRange = NSRange(uri.startIndex..., in: uri)
    guard let match = schemePattern?.firstMatch(in: uri, range: fullRange),
          let range = Range(match.range, in: uri) else { return nil }
    return String(uri[range].dropLast()).lowercased()
  }

  static func isNetwork(_ uri: String) -> Bool {
    guard let scheme = scheme(of: uri) else { return false }
    return networkSchemes.contains(scheme)
  }
}
