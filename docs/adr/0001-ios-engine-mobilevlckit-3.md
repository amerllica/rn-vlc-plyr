# 0001. iOS engine: MobileVLCKit 3.7

Status: accepted (2026-10-07)

## Context

The library must play every container and codec consistently on iOS. Options were MobileVLCKit 3.7 (stable), VLCKit 4 (alpha only) and AVPlayer.

## Decision

Use MobileVLCKit `~> 3.7.4`. All VLCKit calls live in `HybridVlcPlayer` and `HybridVlcPlayerView`, so a later move to VLCKit 4 touches only those files.

## Consequences

Full codec support, same engine as the VLC iOS app. Costs about 40 MB of binary size and an LGPL notice. VLCKit 4 is revisited when it ships a stable release.
