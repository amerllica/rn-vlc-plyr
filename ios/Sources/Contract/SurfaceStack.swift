import Foundation

struct SurfaceStack<Element: AnyObject> {
  private struct WeakEntry {
    weak var element: Element?
  }

  private var entries: [WeakEntry] = []

  var top: Element? {
    entries.last { $0.element != nil }?.element
  }

  var isEmpty: Bool {
    top == nil
  }

  mutating func attach(_ element: Element) {
    detach(element)
    entries.append(WeakEntry(element: element))
  }

  mutating func detach(_ element: Element) {
    entries.removeAll { $0.element == nil || $0.element === element }
  }

  mutating func removeAll() {
    entries.removeAll()
  }
}
