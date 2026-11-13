import Foundation

enum ArtworkRedirect {
  static let maxHops = 5

  private static let http = "http"
  private static let https = "https"

  static func allows(from: URL?, to: URL?) -> Bool {
    let origin = from?.scheme?.lowercased()
    let target = to?.scheme?.lowercased()
    return target == https || (target == http && origin == http)
  }
}
