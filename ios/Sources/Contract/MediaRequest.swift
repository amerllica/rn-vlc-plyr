import Foundation

struct MediaRequest: Equatable {
  let url: URL
  let options: [String]

  static func make(uri: String, userAgent: String? = nil, referrer: String? = nil, vlcOptions: [String]? = nil) -> MediaRequest? {
    guard let url = MediaUri.resolve(uri) else { return nil }
    return MediaRequest(url: url, options: buildOptions(userAgent: userAgent, referrer: referrer, vlcOptions: vlcOptions))
  }

  static func buildOptions(userAgent: String?, referrer: String?, vlcOptions: [String]?) -> [String] {
    var options: [String] = []
    if let userAgent { options.append(":http-user-agent=\(userAgent)") }
    if let referrer { options.append(":http-referrer=\(referrer)") }
    options.append(contentsOf: vlcOptions ?? [])
    return options
  }
}

enum MediaUri {
  static let networkSchemes: Set<String> = ["http", "https", "rtsp", "rtmp", "rtp", "udp", "mms", "ftp", "smb"]

  private static let schemePattern = try? NSRegularExpression(pattern: "^[A-Za-z][A-Za-z0-9+.-]*:")

  static func resolve(_ uri: String) -> URL? {
    if uri.hasPrefix("/") {
      return URL(fileURLWithPath: uri)
    }
    guard scheme(of: uri) != nil else { return nil }
    return URL(string: uri)
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
