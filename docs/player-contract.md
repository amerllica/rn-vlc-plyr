# Player contract

This is the behaviour both native implementations must match exactly. The TypeScript spec in
`src/specs/VlcPlayer.nitro.ts` defines the shapes; this document defines the behaviour. When iOS
and Android disagree, this file decides.

## Defaults

| Member | Default |
|---|---|
| `source` | `undefined` |
| `volume` | `100` |
| `muted` | `false` |
| `rate` | `1` |
| `loop` | `false` |
| `autoPlay` | `true` |
| `timeUpdateInterval` | `250` |
| `subtitleDelay`, `audioDelay` | `0` |
| `status` | `'idle'` |
| `error` | `undefined` |
| `currentTime`, `duration` | `0` |
| `isLive`, `isSeekable` | `false` |
| `videoSize` | `{ width: 0, height: 0 }` |
| `audioTracks`, `subtitleTracks` | `[]` |
| `selectedAudioTrack`, `selectedSubtitleTrack` | `-1` |

## Units and clamping

Native clamps every input. The getter returns the clamped value immediately after the setter runs.

| Input | Rule |
|---|---|
| `volume` | round to integer, clamp 0–100. Engine gain 100 = 0 dB on both OSes |
| `rate` | clamp 0.25–4 |
| `timeUpdateInterval` | round, clamp 50–5000 ms |
| `subtitleDelay`, `audioDelay` | round, clamp −60000–60000 ms. Engine takes microseconds: multiply by 1000 |
| `seek(ms)` | round, clamp to ≥ 0, and to ≤ `duration` when `duration > 0` |
| `seekBy(ms)` | `seek(currentTime + ms)` |
| times | integer milliseconds everywhere |

## Persistence across sources

`volume`, `muted`, `rate`, `loop`, `autoPlay`, `timeUpdateInterval`, `subtitleDelay` and
`audioDelay` belong to the player, not to the media. They survive every source change and are
re-applied to the engine each time a new media reaches `playing`.

## Source

- Setting `source` always reloads, even with an equal value. The JS hook avoids redundant sets.
- Loading resets `error`, `currentTime`, `duration`, `isLive`, `isSeekable`, `videoSize`, tracks and
  selections to defaults, and emits `statusChange('opening')` when playback starts.
- `uri` that is empty or has no scheme and is not an absolute path → `status 'error'`,
  `error { code: 'invalidSource', message: 'Invalid source uri' }`. An absolute path is treated as
  `file://`.
- `userAgent` → `:http-user-agent=…`, `referrer` → `:http-referrer=…`, `vlcOptions` are passed as
  media options verbatim, in that order.
- `autoPlay true` → playback starts at once. `autoPlay false` → media is prepared, status stays
  `'idle'` until `play()`.
- `source = undefined` → engine stops, media is released, all media state resets to defaults,
  `statusChange('idle')`.

## Status machine

Status values: `idle`, `opening`, `buffering`, `playing`, `paused`, `stopped`, `ended`, `error`.

- `statusChange` never fires twice in a row with the same value.
- Engine "opening" → `opening`.
- Engine "buffering" → `buffering` only when the engine is not currently playing. While the engine
  is playing, buffering signals are ignored. This removes the VLC 3 buffering flicker.
- Engine "playing" → `playing`.
- Engine "paused" → `paused`.
- Engine "stopped" after `stop()` → `stopped`. Engine "stopped" caused by the end of media is
  handled as "ended".
- Engine "ended" → when `loop` is false: `ended`, then the `ended` event. When `loop` is true: no
  `ended` status, no `ended` event; playback restarts from 0 and status stays/returns to `playing`.
- Engine error → `error`, then the `error` event.

## Commands

| Command | Rule |
|---|---|
| `play()` | no source → no-op. `ended` or `stopped` → restart from 0. `error` → reload the source and play. Otherwise resume |
| `pause()` | only when `playing` or `buffering`; otherwise no-op |
| `stop()` | engine stop, `currentTime` 0, status `stopped`. No-op when `idle` |
| `seek(ms)` | no-op unless `isSeekable`. In `ended` or `stopped`: restart playback, then seek. Emits one `timeUpdate` right after the seek with the target time |
| `setAudioTrack(id)` / `setSubtitleTrack(id)` | `-1` disables. Unknown id → no-op. A real change emits `tracksChange` |
| `addSubtitle(uri, select)` | no source → throws `Error('addSubtitle requires a source')`. Otherwise adds a subtitle slave; the new track arrives through `tracksChange`. `select` true selects it once it appears |
| `snapshot()` | resolves with a `file://` URI of a PNG of the current frame at video resolution. Rejects with `Error('No video frame available')` when no view is attached or nothing has rendered |
| `release()` | idempotent. Stops, detaches all views, frees the engine. Afterwards every command is a no-op, getters return defaults, no event fires. `dispose()` calls `release()` |

## Events

| Event | When | Payload |
|---|---|---|
| `statusChange` | every status transition | new status |
| `timeUpdate` | while `playing`, at most every `timeUpdateInterval` ms; once after each seek; once with 0 on `stop()` | `{ currentTime, duration }` |
| `loaded` | once per source, right after the first `statusChange('playing')` of that source | `{ duration, isSeekable, isLive, videoSize, audioTracks, subtitleTracks }` |
| `ended` | media ended and `loop` is false, right after `statusChange('ended')` | none |
| `error` | right after `statusChange('error')` | `{ code, message }` |
| `volumeChange` | when `volume` or `muted` actually changes | `{ volume, muted }` |
| `tracksChange` | when either track list or either selection differs from the last emitted value | `{ audioTracks, subtitleTracks, selectedAudioTrack, selectedSubtitleTrack }` |
| `videoSizeChange` | when the decoded size changes and both sides are > 0 | `{ width, height }` |

Listeners may be added at any time. `remove()` is idempotent. Native may emit from any thread;
Nitro delivers callbacks on the JS thread.

## Derived values

- `duration` = engine length in ms, `0` when the engine reports ≤ 0.
- `isSeekable` = engine seekable flag.
- `isLive` = `duration == 0 && !isSeekable`, evaluated once the source is `playing`; `false` before.
- Track lists exclude the engine's "Disable" pseudo-track (id −1). Track ids are engine ids.
  Track names are the engine names; an empty name becomes `Track <id>`.
- Selected ids are the engine's current ids, `-1` when none.

## Errors

| Code | When |
|---|---|
| `invalidSource` | uri rule above fails |
| `network` | engine error before the first `playing` of the source, uri scheme is a network scheme (`http`, `https`, `rtsp`, `rtmp`, `rtp`, `udp`, `mms`, `ftp`, `smb`) |
| `media` | any other engine error |
| `unknown` | reserved, not produced in 1.0 |

Messages are fixed strings so they match across OSes: `Invalid source uri`,
`Could not open the media over the network`, `The media could not be played`.

## View

- `player` attaches this view's video surface to the player. Several views may show one player;
  the most recently attached view gets the video. When it detaches (prop change, unmount, drop),
  the previous still-attached view gets the video back without reloading.
- `resizeMode` (default `contain`): `contain` fits inside with letterbox, `cover` fills and crops,
  `stretch` fills and distorts, `original` shows native pixels centred without scaling.
- `surfaceType` (Android only, default `texture`): `texture` composes like a normal view (clipping,
  rounded corners, transforms); `surface` uses a SurfaceView. iOS ignores it.
- Background is black.

## Audio session (iOS)

On the first `play()` the audio session category is set to `.playback` with mode `.moviePlayback`,
so sound plays with the ring/silent switch on. Background playback is out of scope for 1.0.
