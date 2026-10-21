# 0019. Subtitle style through libVLC instance options

Status: accepted (2026-10-20)

## Context

Apps want to change the subtitle font, size and colours. libVLC 3 has no API for this. The freetype
text renderer reads its `freetype-*` options from the libVLC instance configuration, not from the
media, so the `vlcOptions` media options cannot style subtitles. libVLC 4 adds live text styling, but
both platforms ship libVLC 3.

## Decision

- `VlcPlayer.subtitleStyle` is normalised by pure contract code on each platform and turned into an
  ordered list of `--freetype-*` and `--sub-margin` options. Both platforms test the same list.
- A player with a style owns a private libVLC instance: `VLCMediaPlayer(options:)` on iOS and
  `LibVLC(context, options)` on Android. A player without a style keeps the shared instance.
- A style change on a loaded source reloads the media on a new engine and restores the position,
  the pause state, the added subtitle and audio files ([ADR 0020](0020-external-audio.md)) and the
  track selections. `loaded` is not emitted again.

## Consequences

The style works with the libVLC 3 builds in use. A style change is not instant, and a styled player
uses more memory and builds its own font list the first time it draws text. Embedded ASS and SSA
styling still wins over the style.
