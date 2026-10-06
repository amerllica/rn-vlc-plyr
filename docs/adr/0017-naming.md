# 0017. Public names

Status: accepted (2026-10-07)

## Context

Exports need short, unambiguous names.

## Decision

`useVlcPlayer`, `useVlcPlayerEvent`, `useVlcPlayerStatus`, `VlcPlayerView`, `VlcFullscreenModal` and `Vlc*` types. Native classes follow Nitro: `HybridVlcPlayer`, `HybridVlcPlayerView`.

## Consequences

Mirrors the shape of expo-video's API, which many developers know.
