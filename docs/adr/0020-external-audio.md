# 0020. External audio through playback slaves

Status: accepted (2026-10-21)

## Context

Apps want to play a separate audio file with the video, such as a dub or a commentary track.
libVLC 3 loads extra audio files the same way it loads extra subtitle files: as playback slaves.
`addSubtitle` already used them, with its own lists in each controller and its own rule for keeping
the selection when a subtitle style reloads the media.

## Decision

- `addAudio(uri, select)` mirrors `addSubtitle(uri, select)`: same source check, same URI rules,
  same `tracksChange` flow.
- Audio and subtitle files share one path. A `PlaybackSlave` (kind, URI, select) is stored by each
  controller, handed to the engine, kept on the Android media request and added again after a
  style reload.
- `TrackSelectionRestore` takes the added slaves. For each kind it treats the last tracks of the
  list as the added files and the rest as embedded. A selected embedded track is restored by its
  position once the embedded tracks are back. A selected added file is added again with `select`
  true and is also restored by its position once the whole list is back, because libVLC drops the
  `select` flag of a slave added before the media opens, as iOS does after a style reload.
- Android keeps no queue for calls made before the player exists, as for subtitles.

## Consequences

Both kinds of file use the same code, and the restore rule is tested once per kind. Added tracks
are taken to be the last ones in their list. libVLC 3 adds slave tracks after the tracks the
media already has, so this holds for local files. An adaptive stream that adds tracks later can
break the position restore after a mid-stream style change. A file with more than one audio track
counts as one added file, so `addAudio` expects a file with one audio track.

libVLC 3 ends playback when none of the media's own tracks is selected. With an added audio track
selected, that happens when the video track is off: background audio, the `:no-video` option, or an
audio-only source. Both platforms show it with an MP4 source. On Android, a fresh player after
`loop` or `stop()` keeps the added file but selects the first audio track again.
