# 0022. Reconnect on resume and instance options

Status: accepted (2026-10-27)

## Context

Apps saw a long wait on `play()` after a long pause, most of all on weak networks. The media stays
loaded on `pause()`. While it is paused, the server or a proxy closes the idle HTTP connection. On
play, VLC 3 first drains its buffer and then waits on the dead connection before it opens a new one
(VideoLAN issue 5093). A seek opens a new connection at once. `:http-reconnect` does not help: the
HTTP module that VLC 3 uses for `http://` and `https://` ignores it.

Apps worked around this in JavaScript, but a resume from the lock screen or the notification goes
straight to the native media session and skips that code. The libVLC instance options were also
fixed: the shared instance had none, and a styled player had only the subtitle options.

## Decision

- `reconnectOnResumeAfterMs`: the native controller records each pause. A resume from `paused`
  after at least that long seeks to the current position right before it plays. It skips local
  files, live streams and sources that are not seekable. Off by default.
- `reloadOnStallAfterMs`: a resume arms one check. If the status is still `buffering`, or `playing`
  with no progress since the resume, when it runs, the media reloads in place once with the reload path of [ADR 0019](0019-subtitle-style.md). Off
  by default.
- `libvlcOptions`: instance options that the player merges in front of the subtitle style options.
  Any merged option gives the player a private instance, as in ADR 0019.
- `buffering` event with the buffer fill percent. Android reads it from
  `MediaPlayer.Event.getBuffering()`. MobileVLCKit has no public API for it, so an Objective-C
  observer gets the libVLC player from MobileVLCKit's private `libVLCMediaPlayer` accessor and
  attaches to `libvlc_MediaPlayerBuffering`. Without the accessor the observer is not created.

## Consequences

The fix lives in one place for every resume path. Both delays are off by default, so 1.2 apps keep
their behaviour. VLC 3 reports live HLS as seekable, so an app must keep the reconnect off for live
HLS. The iOS percent depends on a MobileVLCKit internal and can stop on a MobileVLCKit update; the
player keeps working without it.
