# 0010. Release phasing

Status: accepted (2026-10-07)

## Context

Background audio and media session controls need a foreground service, a MediaSession, Now Playing and remote commands, plus real-device testing.

## Decision

1.0 ships the core player. 1.1 adds background audio and the media session. Next and previous buttons appear only when the app listens for `remoteNext` and `remotePrevious`; the library owns no playlist.

## Consequences

The core ships sooner. The 1.1 scope is tracked in a GitHub issue.
