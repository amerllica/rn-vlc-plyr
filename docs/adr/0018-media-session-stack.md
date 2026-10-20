# 0018. Background audio and media session stack

Status: accepted (2026-10-20)

## Context

1.1 adds background audio and system media controls on both platforms (see ADR 0010). Android offers two stacks: `androidx.media` (`MediaSessionCompat` and `NotificationCompat.MediaStyle`) and media3 (`MediaSessionService`). Media3 drives a `Player` interface, so libvlc would need a full adapter that mirrors every state change. Declaring a `mediaPlayback` foreground service in the library manifest would force every app to justify it in Google Play review, even apps that never play in the background. Video cannot render while an iOS app is in the background, and decoding it there wastes battery on Android.

## Decision

- Android uses `androidx.media`. The library owns one `MediaSessionCompat`, a `MediaStyle` notification and a `mediaPlayback` foreground service, `VlcPlaybackService`.
- The library manifest stays empty. Apps opt in through the `rn-vlc-plyr` Expo config plugin or a manifest snippet from the README. iOS apps declare `UIBackgroundModes: audio` the same way. A missing setup logs one warning and never throws.
- In the background, the player disables the video track and the audio continues. iOS keeps the drawable. Android also detaches the video views: a fresh libvlc player created in the background (loop restart, reload) attached to a layout that cannot get a surface stalls the whole pipeline, audio included. On return the views are attached to a fresh layout and the track is enabled again.
- Android players in one process share a single audio focus request, so they do not pause each other. This matches iOS, where every player shares one audio session.
- Both platforms share pure, VLC-free policies for background, interruptions and the media session state, tested side by side.
- The OS shows one Now Playing entry. The last player that starts playing with `nowPlaying` set owns it.

## Consequences

No media3 dependency and no `Player` adapter. Apps that need background playback add one plugin or a few manifest lines. Next and previous stay in the app (ADR 0010).
