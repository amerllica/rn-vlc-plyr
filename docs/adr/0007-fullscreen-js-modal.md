# 0007. Fullscreen as a JS modal

Status: accepted (2026-10-07)

## Context

Native fullscreen would need two implementations that must stay identical.

## Decision

Ship `VlcFullscreenModal`: a React Native `Modal` that hides the status bar, handles the Android back button and renders a second `VlcPlayerView` of the same player.

## Consequences

No native fullscreen code. Screen rotation stays with the app; the README shows a recipe.
