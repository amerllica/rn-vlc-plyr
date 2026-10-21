# Parity report

Results of the cross-platform checklist against [the player contract](player-contract.md).

| | iOS | Android |
|---|---|---|
| Device | iPhone 18 Pro simulator, iOS 27.0 | Pixel_10a emulator |
| Engine | MobileVLCKit 3.7.4 | libvlc-all 3.7.7 |
| React Native | 0.87.1, new architecture | 0.87.1, new architecture |
| Date | 2026-10-07 | 2026-10-07 |

## Native tests

| Suite | iOS | Android |
|---|---|---|
| Pure contract rules | XCTest, pass | JUnit on the JVM, 100 pass |
| Real playback | XCTest with bundled MP4 and M4A files, pass | Instrumented tests on the emulator with bundled MP4, M4A, MKV and SRT files, 43 pass |
| Total | 147 pass | 143 pass |

Counts from 2026-10-21 for 1.2, on the iPhone 18 Pro simulator (iOS 27) and the Pixel_10a emulator
(API 37). The background and media session contract rules have the same 33 test names on both OSes.
The 9 Android subtitle drawing tests are described in
[Android subtitle drawing](#android-subtitle-drawing).

```sh
xcodebuild test -workspace example/ios/RnVlcPlyrExample.xcworkspace -scheme RnVlcPlyr-Unit-Tests -destination 'platform=iOS Simulator,name=iPhone 18 Pro'
cd example/android && ./gradlew :rn-vlc-plyr:testDebugUnitTest :rn-vlc-plyr:connectedDebugAndroidTest
```

## Checklist

| Contract item | iOS | Android | Notes |
|---|---|---|---|
| Defaults and clamping | PASS | PASS | Getters return the clamped value at once. Rounding is `floor(x + 0.5)`. `NaN` is ignored |
| Settings persist across sources | PASS | PASS | Rate 2× confirmed on the next media |
| Source reload and reset | PASS | PASS | `opening` emitted at once with `autoPlay` |
| `autoPlay = false` | PASS | PASS | Status stays `idle` until `play()` |
| Clear source | PASS | PASS | Status `idle`, tracks cleared, audio output released |
| Status deduplication | PASS | PASS | |
| Buffering rule | PASS | PASS | VLC 3 buffering during playback and while paused is ignored |
| `ended` and `loop` | PASS | PASS | With loop on there is no `ended` status or event |
| `play()` from ended, stopped, error | PASS | PASS | Restart from 0, or reload after an error |
| `pause()` | PASS | PASS | |
| `stop()` | PASS | PASS | `statusChange('stopped')`, then `timeUpdate` with 0 |
| `seek` and `seekBy` | PASS | PASS | Clamped. Works while paused. From stopped or ended it restarts and then seeks. One `timeUpdate` with the target |
| Audio and subtitle tracks | PASS | PASS | Matroska test file: 2 audio and 8 subtitle tracks. `-1` disables. Unknown ids are ignored |
| `addAudio` | PASS | PASS | The new track appears after the embedded one and is selected. Throws without a source. A subtitle style reload adds it again and keeps it selected. Playback continues in the background and, on Android, through a new video layout |
| `addSubtitle` | PASS | PASS | The new track appears and is selected. Throws without a source |
| Volume and mute events | PASS | PASS | Repeated values send no event. Android mutes through engine volume 0 |
| `snapshot()` | PASS | PASS | `file://` PNG at video resolution. Rejects with `No video frame available` |
| Resize modes | PASS | PASS | iOS `cover` and `stretch` verified by unit tests only, because the test media match the view's aspect ratio. Android verified visually with 4:3 media |
| Several views, fullscreen hand-back | PASS | PASS | The inline view gets the video back without reloading |
| Error codes | PASS | PASS | Broken URL gives `network`. Bad URI gives `invalidSource` |
| Mount and unmount 20 times, switch sources 20 times | PASS | PASS | No crash. Memory and view count stayed flat |
| HLS on demand | PASS | PASS | Apple `bipbop_4x3` stream |

## Subtitle style (1.2)

Run on 2026-10-20 on the iPhone 17 and iPhone 18 Pro simulators (iOS 27.0) and on the Pixel_10a and
Api35_GoogleApis emulators.

| Check | iOS | Android | Notes |
|---|---|---|---|
| Normalisation and option list | PASS | PASS | Same unit test names and the same option list on both OSes |
| Style set before load | PASS | PASS | Instrumented and XCTest playback |
| Style change while playing | PASS | PASS | Reopens, keeps the position, no second `loaded` |
| Style change while paused | PASS | PASS | Pauses again at the same position |
| Two quick style changes | PASS | PASS | Position from before the first change is kept |
| Styled text on screen | PASS | PASS | 200 % yellow verified visually. Android drew it once the subtitle was added after the style change |
| Added subtitle drawn again after a style change | PASS | PASS | iOS verified visually on HLS. Android verified visually on 2026-10-21 in the release build of the example on the Pixel_10a emulator: an added SRT drawn at 200 % after the reload, with the subtitle track and an added audio track still selected |

## Android subtitle drawing

`SubtitleRenderingTest` plays bundled media in a test activity. The activity hosts the library's
`VideoContainer` in a parent that ignores `requestLayout`, as React Native does. The external SRT has
one magenta cue from 0 to 1 hour and sits in its own folder, so VLC cannot auto-load it next to the
video. The MKV has an embedded SRT track with a new magenta cue every second; the test selects that
track, as an app does. Each test takes emulator screenshots until at least 50 magenta pixels show,
and saves the last screenshot to the test app's external files folder. The class first plays one
subtitle until it shows, because the first subtitle after an install did not draw at all during the
10-second video.

| Case | Texture view | Surface view |
|---|---|---|
| External SRT through `addSubtitle` | PASS | PASS |
| Embedded SRT track in MKV | PASS | PASS |
| External SRT after `stop()` and `play()` | PASS | — |
| External SRT after a switch to `surface` while playing | — | PASS |
| Embedded SRT after a switch to `texture` while playing | PASS | — |
| External SRT after the video track goes off and on | PASS | — |
| External SRT after the video track goes off and on, with an added audio file | PASS | — |

The cue covers about 115 to 135 pixels on a 320 × 640 screen, such as the default emulator in CI,
and far more on a phone-sized screen. The threshold is low enough for both, and an undrawn cue counts
0, because nothing else on screen is magenta.

Run on 2026-10-21 on two emulators, from a clean install of the test app. Api35_GoogleApis
(Android 15) decodes with MediaCodec opaque frames. A new Android 17 emulator (Pixel 9 profile, same
system image as Pixel_10a) used MediaCodec in some runs and software decoding in others. All 36
instrumented tests pass on both. The example app on Android 15 also draws an external SRT in texture
view, in surface view, after a switch while playing, and on the Apple HLS stream.

Before the fix, the last four rows failed with 0 subtitle pixels. The external SRT rows passed before
the fix too. A first run of the embedded rows also passed, but that result was not valid: VLC had
auto-loaded the external SRT from the same folder. VLC does not auto-select the embedded track of the
test MKV, which also has an audio track, so the test now selects it. The failures came from these
causes:

- **A fresh player lost added subtitles.** `play()` after `stop()` or after the end, `loop`, and a
  seek after the end replace the player with the same media. The new media did not get the
  `addSubtitle` files. The engine now adds them to each fresh media as subtitle slaves. A new source,
  `Reload` and `play()` after an error still start without them.
- **A surface type switch while playing left the video black.** The view removed the old layout
  before libVLC detached from it. Then libVLC's `VideoHelper` turned the video track off and on
  again at once, and the "on" step read a stale track id and did nothing. The engine now detaches
  first, then swaps the layout, then turns the video track on again when the old video output is
  gone and the new surfaces exist.
- **A rebuilt video output lost the current cue.** VLC sends each cue to the video output once. When
  the video output is rebuilt during playback (surface switch, video track off and on, a new view),
  the cue on screen is gone until the next cue starts. After the library rebuilds the video output,
  it now seeks to the current time once, if a subtitle track is selected and the media is seekable.
  This sends the current cue again.

The `libvlc vout display: can't get Subtitles Surface` log is not a failure. With opaque MediaCodec
frames, the Android display module asks for a separate subtitle surface. There is none in texture
view, so VLC uses its OpenGL output, which draws the subtitles.

Not fixed here:

- Re-added subtitles are selected on the fresh player, also when the app later chose another
  subtitle track or added the file with `select: false`.
- In surface view, the example app draws the video smaller than the view. Subtitles still draw on the
  video.
- The example app can start playback before the view's surface exists. VLC then logs
  `can't get Video Surface` and decodes in software until the view is ready.

## Leak check

| Check | iOS | Android |
|---|---|---|
| Method | macOS `leaks` on the simulator process after opening and closing the player screen 5 times (each round creates and releases a native player) | Native heap and view count watched in the profiler during 60 mount, unmount and source-switch rounds |
| Result | 14 untyped blocks, 336 bytes in total, none from VLC or this library, not growing with player count. Player threads freed after each close | Native heap and view count flat |

## Known engine limits

These come from VLC 3 and are the same on both platforms.

- **Live HLS reads as not live.** VLC 3 reports live HLS as seekable with a length, so `isLive` is
  `false`.
- **Apple's advanced HLS example** (`img_bipbop_adv_example_hls`) stops after about one second in
  VLCKit 3.7. The example app uses the basic `bipbop_4x3` stream instead.
- **Seeking inside remote MKV files** depends on the file's index. The Matroska test file can land
  on damaged frames after large jumps over HTTP.
- **First subtitle render** builds a font cache. On a fresh install this can take more than 15
  seconds. On Android emulators in October 2026, the first subtitle after an install did not show
  during a 10-second video, even after `buildFontCache()`. Later subtitles showed in 1 to 5 seconds.
- **A newly attached view** stays black while paused or ended, until the next frame is decoded.

## Platform-specific workarounds

- **iOS:** releasing a `VLCMediaPlayer` on the main thread can deadlock, because libvlc waits for the
  video output, which waits for the main thread. Retired players are stopped, kept for one second
  and released on a background queue.
- **iOS:** VLCKit raises an Objective-C exception when a snapshot fails. A small Objective-C helper
  catches it and turns it into a rejected promise.
- **iOS:** the video layer is hidden on every load until the first new frame, so the previous media's
  last frame never shows.
- **Both:** libvlc's stop waits for the input thread, which can hang on a bad network. A freeze
  trace from the emulator showed the main thread stuck in `MediaPlayer.stop()` after leaving a stalled
  stream. Players are therefore never stopped or released on the main thread: each new source, clear,
  stop or release retires the current player to a background thread and continues with a fresh one.
- **Android:** a retired player keeps its video surface until its background shutdown finishes.
  The fresh player therefore gets a new video layout; sharing the old one left the picture black
  after a loop, a reload or a stop followed by play.
- **Android:** libvlc has no mute and no snapshot API. Mute is engine volume 0 with the volume kept;
  snapshots read the `TextureView` bitmap or use `PixelCopy` for `SurfaceView`.
- **Android:** React Native does not lay out views added from native code, so the video container
  measures and lays out its own children.

## Test environment note

During the runs, a long-running Expo Go app in another simulator exhausted the Mac's free network
ports. Remote media then failed at random on both platforms. Local media servers were used to
confirm the behaviour, and remote media passed whenever the network worked.

## Background and media session (1.1)

Manual run on real devices. Status is "not run" until the run is recorded.

Smoke run on 2026-10-20 with the bare example, before the real-device run:

- Android, Pixel_10a emulator (API 37). The foreground service ran with type `mediaPlayback`. The
  Quick Settings player showed the title, artist, artwork, seek bar, skip buttons, and next and
  previous only while the listeners were on. Audio continued in the background, including across
  loop restarts. Video came back on return.
- iOS, iPhone 18 Pro simulator (iOS 27). The settings reached the native player and playback
  survived a screen lock. The simulator has no Control Center media module, so Now Playing was
  checked only by the XCTest suite.

| Check | iOS | Android | Notes |
|---|---|---|---|
| Background off: player pauses on Home, stays paused on return | not run | not run | |
| Background on: audio continues on Home and screen lock | not run | not run | |
| Background on: video track off in background, back on return | not run | not run | |
| Lock screen shows title, artist, artwork, position | not run | not run | |
| Control Center / notification shade controls | not run | not run | |
| Notification seek bar | — | not run | |
| Quick Settings media player | — | not run | Android 11 and newer |
| Skip back and forward use `remoteSkipInterval` | not run | not run | |
| Next and previous appear only with listeners | not run | not run | |
| Phone call pauses, playback resumes after the call | not run | not run | |
| User pause before a call is not undone | not run | not run | |
| Headphones unplugged pauses | not run | not run | |
| Bluetooth headset play, pause, next | not run | not run | |
| CarPlay basic controls | not run | — | |
| Android Auto basic controls | — | not run | |
| Foreground service starts on Android 14 and the app survives 10 minutes locked | — | not run | |
| Missing setup logs one warning and plays in the foreground | not run | not run | |
| Artwork shows; a failing URL retries 4 times, then logs one warning | not run | not run | |

