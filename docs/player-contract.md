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
| `staysActiveInBackground` | `false` |
| `nowPlaying` | `undefined` |
| `remoteSkipInterval` | `10000` |
| `androidNotification` | `undefined` |
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
| `NaN` | ignored by every setter and by `seek`/`seekBy`; the previous value stays |
| rounding | JavaScript `Math.round` semantics: `floor(x + 0.5)`, so −0.5 becomes 0 |
| `volume` | round to integer, clamp 0–100. Engine gain 100 = 0 dB on both OSes |
| `rate` | clamp 0.25–4 |
| `timeUpdateInterval` | round, clamp 50–5000 ms |
| `subtitleDelay`, `audioDelay` | round, clamp −60000–60000 ms. Engine takes microseconds: multiply by 1000 |
| `remoteSkipInterval` | round, clamp 1000–60000 ms |
| `seek(ms)` | round, clamp to ≥ 0, and to ≤ `duration` when `duration > 0` |
| `seekBy(ms)` | `seek(currentTime + ms)` |
| times | integer milliseconds everywhere |

## Persistence across sources

`volume`, `muted`, `rate`, `loop`, `autoPlay`, `timeUpdateInterval`, `subtitleDelay`,
`audioDelay`, `staysActiveInBackground`, `nowPlaying`, `remoteSkipInterval` and
`androidNotification` belong to the player, not to the media. They survive every source change and are
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
- Engine "buffering" → `buffering` only when the engine is not playing and the status is not
  `paused`. Otherwise buffering signals are ignored. This removes the VLC 3 buffering flicker and
  keeps a paused player paused while it seeks.
- Engine "playing" → `playing`.
- Engine "paused" → `paused`.
- The engine "stopped" signal is ignored. VLC sends it after the end of media and on source
  switches. The `stopped` status comes only from `stop()`.
- Engine "ended" → when `loop` is false: `ended`, then the `ended` event. When `loop` is true: no
  `ended` status, no `ended` event; playback restarts from 0 and status stays/returns to `playing`.
- Engine error → `error`, then the `error` event.

## Commands

| Command | Rule |
|---|---|
| `play()` | no source → no-op. `ended` or `stopped` → restart from 0. `error` → reload the source and play. Otherwise resume |
| `pause()` | only when `playing` or `buffering`; otherwise no-op |
| `stop()` | engine stop, `currentTime` 0, status `stopped`. Works from every status except `idle`, including `ended` and `error` |
| `seek(ms)` | no-op unless `isSeekable`. In `ended` or `stopped`: restart playback, then seek. Emits one `timeUpdate` right after the seek with the target time |
| `setAudioTrack(id)` / `setSubtitleTrack(id)` | `-1` disables. Unknown id → no-op. A real change emits `tracksChange` |
| `addSubtitle(uri, select)` | no source → throws `Error('addSubtitle requires a source')` (Nitro prefixes synchronous errors with `VlcPlayer.addSubtitle(...): `). Otherwise adds a subtitle slave; the new track arrives through `tracksChange`. `select` true selects it once it appears |
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
| `remoteNext` | the system next command fires (lock screen, notification, headset, car) | none |
| `remotePrevious` | the system previous command fires | none |

Ordering rules:

- `stop()` emits `statusChange('stopped')`, then `timeUpdate` with `currentTime` 0.
- A load with `autoPlay` emits `statusChange('opening')` at once.
- An invalid source set while the status is already `error` emits nothing; the `error` getter still
  updates.
- `tracksChange` is evaluated on every load, so an empty list is emitted when the previous list was
  not empty. Track info is not refreshed while `ended`, `stopped` or during a loop restart.
- `videoSizeChange` deduplication resets on every new source.

Listeners may be added at any time. `remove()` is idempotent. Native may emit from any thread;
Nitro delivers callbacks on the JS thread.

## Derived values

- `duration` = engine length in ms, `0` when the engine reports ≤ 0.
- `isSeekable` = engine seekable flag.
- `isLive` = `duration == 0 && !isSeekable`, evaluated once the source is `playing`; `false` before.
  VLC 3 reports live HLS as seekable with a length, so live HLS reads as not live on both OSes.
- Engine seekable and length updates are accepted only after the current source has reached
  `playing` once, and only while the status is `buffering`, `playing` or `paused`. While opening,
  VLC still reports the previous media's values. The values are kept on `stop()`, so seeking after
  `stop()` still works.
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
so sound plays with the ring/silent switch on. The session is deactivated with
`.notifyOthersOnDeactivation` when the source is cleared and on `release()`.

## Background

"Active" means the status is `opening`, `buffering` or `playing`.

| Situation | Rule |
|---|---|
| App enters background, `staysActiveInBackground` false | pause when active. Nothing resumes on return |
| App enters background, `staysActiveInBackground` true | audio continues. The video track is disabled. Android also detaches the video views, because a surface cannot be created while the app is stopped |
| App returns to foreground | a disabled video track is enabled again. Android first attaches the views to a fresh video layout |
| A fresh engine player is created in background (load, loop, reload, stop) | the disabled video track is applied to it too. Android attaches no views to it |
| `staysActiveInBackground` set to false while in background | pause when active |
| `staysActiveInBackground` set to true while in background | disable the video track |

The app must declare background playback: `UIBackgroundModes` with `audio` on iOS, and
`com.margelo.nitro.rnvlcplyr.VlcPlaybackService` with `foregroundServiceType="mediaPlayback"` plus
the `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permissions on Android. The library
declares none of these. When `staysActiveInBackground` is set to true and the setup is missing,
native logs one warning per process, starting with `rn-vlc-plyr: staysActiveInBackground needs`,
and never throws.

Android foreground service:

- Starts when the session owner reaches `playing` while `staysActiveInBackground` is true.
- Leaves the foreground on `paused`, `stopped` and `ended`. The notification stays.
- Stops when the media session becomes inactive.
- Swiping the app away from recents stops playback and the service.

## Audio focus and interruptions

| Event | iOS signal | Android signal | Rule |
|---|---|---|---|
| Interruption begins | `AVAudioSession` interruption `.began` | transient audio focus loss | pause when active and remember that the library paused |
| Interruption ends | interruption `.ended` with `.shouldResume` | `AUDIOFOCUS_GAIN` | resume only if the library paused for this interruption |
| Interruption ends without resume | `.ended` without `.shouldResume` | | stay paused, forget the interruption |
| Permanent loss | | `AUDIOFOCUS_LOSS` | pause when active, never resume |
| Headphones unplugged | route change `.oldDeviceUnavailable` | `ACTION_AUDIO_BECOMING_NOISY` | pause when active, never resume |
| Ducking | system ducks | system ducks (`willPauseWhenDucked` false) | no pause |

Any user command (`play`, `pause`, `stop`, a source change, `release`, or a remote play or pause)
and any background pause forget a pending interruption, so a user pause is never undone.

Android requests audio focus (`AUDIOFOCUS_GAIN`, usage media, content movie) on every play or
resume. All players in the process share one focus request, the same way all iOS players share one
audio session, so one player never pauses another. Pausing keeps focus. Focus is abandoned on `stop()`, on source clear and on `release()`. When
the request is denied, playback still starts without focus and the next play asks again. Android 15
denies focus to an app that is neither the top app nor running a foreground service, for example a
paused app resumed from the notification. iOS behaves the same way: a failed session activation does
not block playback.

## Media session

- The session is active while `nowPlaying` is set **and** a source is set. `source.title` does not
  create a session. `nowPlaying = undefined`, `source = undefined` and `release()` clear it.
- The OS has one Now Playing slot. A player becomes the owner when it reaches `playing` with an
  active session. When the owner's session becomes inactive, the system entry is cleared. Remote
  commands go to the owner only.

| Status | Session playback state |
|---|---|
| `idle` | none |
| `opening`, `buffering` | buffering |
| `playing` | playing |
| `paused` | paused |
| `stopped`, `ended` | stopped |
| `error` | error |

- Position is `currentTime`, duration is `duration`. Both are 0 when live. The playback rate is
  `rate` while `playing` and 0 otherwise; the OS extrapolates the position from it.
- The system entry is refreshed on every status change, `loaded`, seek, loop restart (position 0),
  rate change, `nowPlaying` change, `remoteSkipInterval` change, next or previous listener count change, source change and
  `release()`. It is not refreshed on every `timeUpdate`.
- iOS writes `MPNowPlayingInfoCenter`: title, artist, album, artwork, duration, elapsed time, rate
  and the live flag. This drives the lock screen, Control Center and the Dynamic Island.
- Android writes `MediaSessionCompat` metadata and playback state, and posts a `MediaStyle`
  notification on channel `rn_vlc_plyr_playback`. It appears in the notification shade, on the lock
  screen and in the Quick Settings player, with artwork as the large icon and a seek bar.
  `androidNotification.color` is a hex colour; an invalid value is ignored. `smallIcon` is a
  drawable resource name in the app; the default is the app icon.
- `artworkUri` takes `https://`, `http://` and `file://`, and `content://` on Android. Plain
  `http://` works only when the app allows cleartext traffic (an iOS App Transport Security
  exception, Android `usesCleartextTraffic`).
- Artwork loads off the main thread. A load succeeds only with an HTTP 2xx reply, after redirects,
  whose body decodes as an image. It is downscaled to 512 px on the long side. Connect and read each
  time out after 10 s.
- A failed load is retried after 1 s, 3 s and 9 s, four attempts in all. After the last failure the
  URI is marked failed and native logs one warning:
  `rn-vlc-plyr: artwork could not be loaded from <uri>: <reason>`. Changing `nowPlaying` to a different
  value starts a new set of attempts. To retry the same value, set `nowPlaying` to `undefined`, then
  back.
- The last loaded image is cached per process, keyed by URI. Until the image arrives, the system entry
  shows without artwork; it is published again when the image arrives.

## Remote commands

| Command | Available | Action |
|---|---|---|
| play | always | `play()` |
| pause | always | `pause()` |
| toggle play/pause | always | `pause()` when `playing` or `buffering`, otherwise `play()` |
| stop | always (Android) | `stop()` |
| seek (scrubber) | `isSeekable` and not `isLive` | `seek(position)` |
| skip back, skip forward | `isSeekable` and not `isLive` | `seekBy(∓remoteSkipInterval)` |
| next | while a `remoteNext` listener exists | emits `remoteNext` only |
| previous | while a `remotePrevious` listener exists | emits `remotePrevious` only |

The library owns no playlist. Bluetooth headset, CarPlay and Android Auto buttons map to the same
commands.
