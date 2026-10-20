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
| Pure contract rules | XCTest, pass | JUnit on the JVM, 77 pass |
| Real playback | XCTest with a bundled 2-second MP4, pass | Instrumented tests on the emulator with a bundled 2-second MP4, 20 pass |
| Total | 111 pass | 97 pass |

Counts updated on 2026-10-20 for 1.1. The background and media session contract rules have the same
33 test names on both OSes.

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
| Added subtitle drawn again after a style change | PASS | not confirmed | iOS verified visually on HLS. Android restores and selects the track (instrumented test), but the emulators did not draw subtitles reliably even without a style: libVLC logs `can't get Subtitles Surface` |

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
  seconds.
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

