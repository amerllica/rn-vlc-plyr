import Foundation

final class ListenerRegistry<Payload> {
  typealias Listener = (Payload) -> Void
  typealias PresenceHandler = (Bool) -> Void

  private struct Entry {
    let id: UInt64
    let listener: Listener
  }

  private struct State {
    var nextId: UInt64 = 0
    var entries: [Entry] = []
    var onPresenceChange: PresenceHandler?
  }

  private let state = Locked(State())

  var count: Int {
    state.read { $0.entries.count }
  }

  var hasListeners: Bool {
    count > 0
  }

  func observePresence(_ handler: @escaping PresenceHandler) {
    state.mutate { $0.onPresenceChange = handler }
  }

  func add(_ listener: @escaping Listener) -> VlcListenerSubscription {
    var id: UInt64 = 0
    changeEntries { state in
      state.nextId += 1
      id = state.nextId
      state.entries.append(Entry(id: id, listener: listener))
    }
    return VlcListenerSubscription(remove: { [weak self, id] in
      self?.remove(id)
    })
  }

  func emit(_ payload: Payload) {
    let listeners = state.read { $0.entries.map(\.listener) }
    listeners.forEach { $0(payload) }
  }

  func removeAll() {
    changeEntries { $0.entries.removeAll() }
  }

  private func remove(_ id: UInt64) {
    changeEntries { $0.entries.removeAll { $0.id == id } }
  }

  private func changeEntries(_ body: (inout State) -> Void) {
    let change = state.mutate { state -> (handler: PresenceHandler?, before: Bool, after: Bool) in
      let before = !state.entries.isEmpty
      body(&state)
      return (state.onPresenceChange, before, !state.entries.isEmpty)
    }
    guard change.before != change.after else { return }
    change.handler?(change.after)
  }
}
