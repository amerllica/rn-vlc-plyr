import Foundation

final class ListenerRegistry<Payload> {
  typealias Listener = (Payload) -> Void

  private struct Entry {
    let id: UInt64
    let listener: Listener
  }

  private struct State {
    var nextId: UInt64 = 0
    var entries: [Entry] = []
  }

  private let state = Locked(State())

  var count: Int {
    state.read { $0.entries.count }
  }

  func add(_ listener: @escaping Listener) -> VlcListenerSubscription {
    let id = state.mutate { state -> UInt64 in
      state.nextId += 1
      state.entries.append(Entry(id: state.nextId, listener: listener))
      return state.nextId
    }
    return VlcListenerSubscription(remove: { [weak self] in
      self?.remove(id)
    })
  }

  func emit(_ payload: Payload) {
    let listeners = state.read { $0.entries.map(\.listener) }
    listeners.forEach { $0(payload) }
  }

  func removeAll() {
    state.mutate { $0.entries.removeAll() }
  }

  private func remove(_ id: UInt64) {
    state.mutate { $0.entries.removeAll { $0.id == id } }
  }
}
