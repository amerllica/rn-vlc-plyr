# 0006. Player object plus view

Status: accepted (2026-10-07)

## Context

Options were a player object with a separate view, a view with ref methods, and a pure props API.

## Decision

`useVlcPlayer()` owns a Nitro HybridObject. `<VlcPlayerView player={player} />` only draws it.

## Consequences

One player can be shown by several views without reloading, which makes fullscreen a JS concern. The hook releases the player on unmount.
