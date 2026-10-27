# Changelog

All notable changes to rn-vlc-plyr. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/). Each section is the body of its GitHub release.

## [Unreleased]

## [1.3.0] - 2026-10-27

### Highlights

Faster play after a long pause on a slow network, and a buffering percent you can show instead of
an endless spinner. The player can open a fresh connection when you resume, and can reload a stream
that never comes back. All of it is off by default and works the same on iOS and Android.

### Added

- `player.reconnectOnResumeAfterMs`: after a pause at least this long, `play()` seeks to the
  current position before it plays, so VLC opens a new connection at once instead of waiting on the
  one the server closed. It covers every resume, also from the lock screen, the notification and
  remote commands, and skips local files, live streams and sources that are not seekable.
  See [ADR 0022](docs/adr/0022-resume-reconnect.md).
- `player.reloadOnStallAfterMs`: when playback has not moved this long after a resume, the media
  reloads once at the same position, with the added files and the selected tracks.
- `player.libvlcOptions`: libVLC instance options such as `--network-caching=3000`. They merge with
  the subtitle style options, and a change reloads the media in place.
- `buffering` event with `{ percent }`, the buffer fill from 0 to 100.
- Example apps: a network section with the reconnect and stall delays and a large cache switch, and
  the buffering percent in the API lab and the event log.

### Known limits

- VLC 3 reports live HLS as seekable, so `reconnectOnResumeAfterMs` does not skip it. Keep it at `0`
  for live HLS.
- iOS: MobileVLCKit has no public buffering percent. The library reads it from the libVLC player
  under MobileVLCKit. If a MobileVLCKit update removes that access, `buffering` stops firing on iOS.
- A player with `libvlcOptions` uses its own libVLC instance, which costs extra memory.

### Docs

- README: the new properties and event, `libvlcOptions` next to `vlcOptions`, and a recipe for
  resuming after a long pause.
- Player contract: instance options, resume and stalls, and the `buffering` event.
- [ADR 0022](docs/adr/0022-resume-reconnect.md): reconnect on resume and instance options.
- Parity report: the iOS buffering percent and the 1.3 test counts.

### Internal

- GitHub releases take their notes from this changelog, and CI fails when the package version has
  no section. See [ADR 0021](docs/adr/0021-release-notes.md).
- CI runs a failed iOS test once more before it fails the job.
- CI uploads the Android instrumented test reports and the iOS XCTest result bundle when a native
  test job fails.
- The iOS playback tests send the test video to a dummy output.

## [1.2.0] - 2026-10-21

### Highlights

Play a separate audio file with the video, such as a dub or a commentary track, and style the
subtitles with your own font size, colours, outline, shadow and margin. Both work the same on iOS
and Android.

### Added

- `player.addAudio(uri, select)` adds an external audio file (`.m4a`, `.mp3`, `.aac` and more) as an
  extra audio track. It works like `addSubtitle`: same URI rules, and the new track comes through
  `tracksChange`. See [ADR 0020](docs/adr/0020-external-audio.md).
- `player.subtitleStyle` sets the font, size, weight, colour and opacity, outline, shadow,
  background box and bottom margin of text subtitles (SRT, WebVTT and plain text). A style change
  reloads the media, keeps the position, the pause state, the added files and the selected tracks,
  and does not emit `loaded` again. See [ADR 0019](docs/adr/0019-subtitle-style.md).
- Example apps: an external audio field in the audio section and subtitle style controls.

### Fixed

- Now Playing artwork that fails to load is retried with the same schedule on iOS and Android.
- Android: added subtitle files stay after the player is replaced, for example on `loop` or after
  `stop()`.
- Android: video and subtitles come back after the video output changes, for example when you
  enter or leave fullscreen.

### Changed

- A media with an added audio file opens with the FFmpeg reader (`:demux=avformat`). The first
  `addAudio` call reloads the media once. For adaptive streams this replaces VLC's adaptive reader.
  With it, playback keeps going when the video track is off, for example in the background, with
  `:no-video` or with an audio-only source.

### Known limits

- `addAudio` expects a file with one audio track. A file with more audio tracks counts as one added
  file.
- Android: after `loop` or `stop()` a fresh player keeps the added audio file but selects the first
  audio track again.
- Embedded ASS and SSA styling still wins over `subtitleStyle`.

### Docs

- README: `addAudio`, subtitle style, the artwork loading rules and the release checks.
- Player contract: subtitle style and the shared playback slave restore rules.
- Parity report: Android subtitle drawing results and the 1.2 test counts.

### Internal

- On-device subtitle drawing checks on the Android emulator.
- The package keywords now include `audio` and `audio-tracks`.

## [1.1.0] - 2026-10-20

### Highlights

Background audio and system media controls. Audio keeps playing on a locked screen or with the app
in the background, and the lock screen, Control Center, the Android notification shade and headset
buttons control the player, with one API on both platforms.

### Added

- `player.staysActiveInBackground` keeps the audio playing when the app goes to the background. The
  video track turns off in the background and comes back when the app returns.
- `player.nowPlaying` (`{ title, artist?, album?, artworkUri? }`) shows the media on the lock screen,
  in Control Center and in the Android media notification. The last player that starts playing
  with `nowPlaying` set owns the system entry.
- `player.remoteSkipInterval` sets the step of the skip forward and skip back buttons
  (1000 to 60000 ms, default 10000).
- `remoteNext` and `remotePrevious` events. The next and previous buttons appear only while the
  app listens for one of them; the library owns no playlist.
- iOS: Now Playing, remote commands and audio session interruptions.
- Android: a media session, a `MediaStyle` playback notification and the `VlcPlaybackService`
  foreground service. All players in one app share one audio focus request, and playback still
  starts when the system denies audio focus.
- An Expo config plugin. Add `"plugins": ["rn-vlc-plyr"]` to `app.json` to declare the iOS audio
  background mode and the Android foreground service.
- Example apps: a background section.

### Setup

The library declares nothing in your app by itself. Background playback needs the Expo plugin, or
the `Info.plist` and `AndroidManifest.xml` entries from the README. Without them the player logs one
warning and plays in the foreground only. See
[ADR 0018](docs/adr/0018-media-session-stack.md).

### Docs

- README: background audio and lock screen controls, with the setup for Expo, bare iOS and bare
  Android.
- Player contract: background, audio focus and interruptions, media session and remote commands.

## [1.0.1] - 2026-10-08

### Highlights

A fix for Android video that went black after a restart.

### Fixed

- Android: a fresh player gets a new video layout, so video shows again after `loop`, a source
  change or `stop()`.

### Docs

- Parity report: the Android video layout handoff.

## [1.0.0] - 2026-10-07

### Highlights

A full rewrite. rn-vlc-plyr now runs on Nitro Modules with Swift and Kotlin, uses VLC 3.7 on both
platforms, and follows one written behaviour contract, so every command, event and value is the
same on iOS and Android.

### Breaking

- The 0.2 `RnVlcPlyr` component and its props and methods are gone. Create a player with
  `useVlcPlayer(source)` and show it with `<VlcPlayerView player={player} />`. See the README
  quick start.
- The new architecture is required. Support starts at React Native 0.79, Expo SDK 53 with a
  development build, iOS 16 and Android API 24. See [ADR 0005](docs/adr/0005-support-floor.md).
- `react-native-nitro-modules` 0.37 or newer is a peer dependency.

### Added

- `useVlcPlayer(source, setup?)` and `createVlcPlayer(source?, setup?)` create a player object.
  One player can be shown by several views, so fullscreen never reloads the media.
- `<VlcPlayerView>` with `resizeMode` (`contain`, `cover`, `stretch`, `original`) and, on Android,
  `surfaceType` (`texture` or `surface`).
- `<VlcFullscreenModal>`: a fullscreen modal that takes your own overlay controls as children and
  closes on the Android back button.
- Playback: `play`, `pause`, `stop`, `seek`, `seekBy`, `volume` (0 to 100), `muted`, `rate`
  (0.25 to 4), `loop` and `autoPlay`.
- Tracks: audio and subtitle track lists and selection, `addSubtitle(uri, select)` for external
  `.srt`, `.ass`, `.ssa` and `.vtt` files, `subtitleDelay` and `audioDelay`.
- `snapshot()` returns a `file://` PNG of the current frame.
- Sources: `uri`, `userAgent`, `referrer`, `title` and raw `vlcOptions`.
- Events through `useVlcPlayerEvent`: `statusChange`, `timeUpdate`, `loaded`, `ended`, `error`,
  `volumeChange`, `tracksChange` and `videoSizeChange`.
- Read-only state: `status`, `error`, `currentTime`, `duration`, `isLive`, `isSeekable` and
  `videoSize`. All times are in milliseconds.
- Engines: MobileVLCKit 3.7 on iOS and libvlc-android 3.7 on Android.
- Native players are stopped and released off the main thread on both platforms, so a source
  change does not block the UI. Each iOS load gets a fresh VLC player.
- Two example apps, bare React Native and Expo, that share one UI.

### Docs

- README with installation, API reference, recipes, limitations and troubleshooting.
- [Player contract](docs/player-contract.md): the shared behaviour rules.
- [Parity report](docs/parity.md): native test results and the leak check.
- [Architecture decision records](docs/adr/README.md).
- iOS 27: apps must adopt the scene lifecycle. The Expo example shows how.

### Internal

- Jest tests for the JS layer, XCTest and JUnit tests for the contract rules, and playback tests
  on an iOS simulator and an Android emulator in CI.
- Releases are published only by GitHub Actions on a version tag, with npm trusted publishing and
  provenance. See [ADR 0012](docs/adr/0012-release-pipeline.md).

[Unreleased]: https://github.com/amerllica/rn-vlc-plyr/compare/v1.3.0...HEAD
[1.3.0]: https://github.com/amerllica/rn-vlc-plyr/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/amerllica/rn-vlc-plyr/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/amerllica/rn-vlc-plyr/compare/v1.0.1...v1.1.0
[1.0.1]: https://github.com/amerllica/rn-vlc-plyr/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/amerllica/rn-vlc-plyr/commits/v1.0.0
