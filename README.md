# rn-vlc-plyr

<p>
  <a href="https://www.npmjs.com/package/rn-vlc-plyr"><img alt="npm" src="https://img.shields.io/npm/v/rn-vlc-plyr.svg"></a>
  <a href="https://github.com/amerllica/rn-vlc-plyr/actions/workflows/ci_cd.yml"><img alt="CI" src="https://github.com/amerllica/rn-vlc-plyr/actions/workflows/ci_cd.yml/badge.svg"></a>
  <img alt="platforms" src="https://img.shields.io/badge/platforms-iOS%20%7C%20Android-blue.svg">
  <img alt="new architecture" src="https://img.shields.io/badge/new%20architecture-only-brightgreen.svg">
  <a href="LICENSE"><img alt="license" src="https://img.shields.io/npm/l/rn-vlc-plyr.svg"></a>
</p>

A video and audio player for React Native built on the **VLC** engine. It plays almost any container
and codec (MKV, MP4, AVI, MOV, WebM, TS, FLV, HLS, RTSP, HEVC, AV1, AC3, DTS, ASS/SSA/SRT subtitles,
and more) with **the same API and the same behaviour on iOS and Android**.

- **VLC everywhere.** MobileVLCKit 3.7 on iOS, libvlc-android 3.7 on Android.
- **Identical on both platforms.** Every command, event and value follows one written
  [behaviour contract](docs/player-contract.md). Units are always milliseconds; volume is always 0–100.
- **Modern React Native.** Built with [Nitro Modules](https://nitro.margelo.com/): Swift and Kotlin,
  JSI, synchronous getters. New architecture only.
- **Bare React Native and Expo.** Works in both (Expo needs a development build, not Expo Go).
- **Player object plus view.** One player can be shown by several views, so fullscreen never reloads
  the media.

## Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Quick start](#quick-start)
- [API](#api)
- [Recipes](#recipes)
- [Limitations](#limitations)
- [Troubleshooting](#troubleshooting)
- [Licensing](#licensing)

## Requirements

| | Minimum |
|---|---|
| React Native | 0.79, new architecture |
| Expo SDK | 53, development build |
| iOS | 16.0 |
| Android | API 24 (Android 7.0) |
| react-native-nitro-modules | 0.37 |

The old architecture, Expo Go and web are not supported.

## Installation

### Bare React Native

```sh
yarn add rn-vlc-plyr react-native-nitro-modules
cd ios && pod install
```

### Expo

```sh
npx expo install rn-vlc-plyr react-native-nitro-modules
npx expo prebuild
npx expo run:ios
npx expo run:android
```

No config plugin is needed for 1.0.

### App size on Android

libvlc ships native code for four ABIs, about 25 MB each. Ship only what you need, or use app
bundles so Google Play delivers one ABI per device:

```groovy
android {
  defaultConfig {
    ndk {
      abiFilters "arm64-v8a", "armeabi-v7a"
    }
  }
}
```

## Quick start

```tsx
import { StyleSheet, Button, View } from 'react-native';
import { VlcPlayerView, useVlcPlayer, useVlcPlayerStatus } from 'rn-vlc-plyr';

export function Player() {
  const player = useVlcPlayer('https://example.com/movie.mkv', (p) => {
    p.loop = true;
  });
  const status = useVlcPlayerStatus(player);

  return (
    <View>
      <VlcPlayerView player={player} style={styles.video} resizeMode="contain" />
      <Button
        title={status === 'playing' ? 'Pause' : 'Play'}
        onPress={() => (status === 'playing' ? player.pause() : player.play())}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  video: { width: '100%', aspectRatio: 16 / 9 },
});
```

## API

### `useVlcPlayer(source, setup?)`

Creates a player for the lifetime of the component and releases it on unmount.

- `source`: a URL string, a [`VlcSource`](#vlcsource) object, or `null`/`undefined` for no media.
  When the source changes, the same player loads the new media.
- `setup(player)`: runs once, right after creation and **before** the first source is assigned. Set
  `autoPlay`, `volume`, `loop` and so on here.

`createVlcPlayer(source?, setup?)` does the same outside React. Call `player.release()` yourself when
done.

### `VlcSource`

| Field | Type | Notes |
|---|---|---|
| `uri` | `string` | `https://`, `http://`, `file://`, `content://`, `rtsp://`, `rtmp://` or an absolute path |
| `userAgent` | `string?` | HTTP user agent |
| `referrer` | `string?` | HTTP referrer |
| `vlcOptions` | `string[]?` | Raw VLC media options, for example `[':network-caching=3000']` |
| `title` | `string?` | Display title |

Bundled assets: pass `Image.resolveAssetSource(require('./clip.mp4')).uri`.

### `VlcPlayer`

Settable properties. They persist across source changes and are clamped natively.

| Property | Type | Default | Range |
|---|---|---|---|
| `source` | `VlcSource \| undefined` | `undefined` | |
| `volume` | `number` | `100` | 0–100, integer |
| `muted` | `boolean` | `false` | independent of `volume` |
| `rate` | `number` | `1` | 0.25–4 |
| `loop` | `boolean` | `false` | |
| `autoPlay` | `boolean` | `true` | |
| `timeUpdateInterval` | `number` | `250` | 50–5000 ms |
| `subtitleDelay` | `number` | `0` | −60000–60000 ms |
| `audioDelay` | `number` | `0` | −60000–60000 ms |

Read-only properties:

| Property | Type | Notes |
|---|---|---|
| `status` | `VlcStatus` | `'idle' \| 'opening' \| 'buffering' \| 'playing' \| 'paused' \| 'stopped' \| 'ended' \| 'error'` |
| `error` | `VlcError \| undefined` | `{ code: 'invalidSource' \| 'network' \| 'media' \| 'unknown', message }` |
| `currentTime` | `number` | ms |
| `duration` | `number` | ms, `0` when unknown or live |
| `isLive` | `boolean` | no duration and not seekable |
| `isSeekable` | `boolean` | |
| `videoSize` | `{ width, height }` | decoded size in pixels |
| `audioTracks` | `VlcTrack[]` | `{ id, name }` |
| `subtitleTracks` | `VlcTrack[]` | `{ id, name }` |
| `selectedAudioTrack` | `number` | `-1` when none |
| `selectedSubtitleTrack` | `number` | `-1` when off |

Methods:

| Method | Notes |
|---|---|
| `play()` | Resumes, or restarts from 0 after `ended`/`stopped`, or retries after `error` |
| `pause()` | |
| `stop()` | Position goes back to 0 |
| `seek(ms)` | Absolute position; ignored when not seekable |
| `seekBy(ms)` | Relative, for example `seekBy(-10000)` |
| `setAudioTrack(id)` | `-1` disables audio |
| `setSubtitleTrack(id)` | `-1` hides subtitles |
| `addSubtitle(uri, select)` | Adds an external `.srt`, `.ass`, `.ssa` or `.vtt` file |
| `snapshot()` | `Promise<string>` with a `file://` PNG of the current frame |
| `release()` | Frees the native player. `useVlcPlayer` does this for you |

### Events

Subscribe with `useVlcPlayerEvent(player, event, listener)` in components, or
`addVlcPlayerListener(player, event, listener)` anywhere (returns `{ remove() }`).

| Event | Payload | When |
|---|---|---|
| `statusChange` | `VlcStatus` | Every status transition |
| `timeUpdate` | `{ currentTime, duration }` | While playing, every `timeUpdateInterval` ms, and after each seek |
| `loaded` | `{ duration, isSeekable, isLive, videoSize, audioTracks, subtitleTracks }` | Once per source, when it first plays |
| `ended` | none | Media finished and `loop` is off |
| `error` | `VlcError` | Playback failed |
| `volumeChange` | `{ volume, muted }` | `volume` or `muted` changed |
| `tracksChange` | `{ audioTracks, subtitleTracks, selectedAudioTrack, selectedSubtitleTrack }` | Tracks or selection changed |
| `videoSizeChange` | `{ width, height }` | Decoded size changed |

`useVlcPlayerStatus(player)` returns the current status and re-renders on change.

### `<VlcPlayerView>`

Accepts all `View` props plus:

| Prop | Type | Default | Notes |
|---|---|---|---|
| `player` | `VlcPlayer` | | The player to show |
| `resizeMode` | `'contain' \| 'cover' \| 'stretch' \| 'original'` | `'contain'` | |
| `surfaceType` | `'texture' \| 'surface'` | `'texture'` | Android only. `texture` supports clipping, rounded corners and transforms. `surface` uses less GPU but always draws on top in its own layer |

### `<VlcFullscreenModal>`

A full-screen `Modal` that shows the same player without reloading.

| Prop | Type | Notes |
|---|---|---|
| `player` | `VlcPlayer` | |
| `visible` | `boolean` | |
| `onClose` | `() => void` | Also called by the Android back button |
| `resizeMode`, `surfaceType` | | As on `VlcPlayerView` |
| `backgroundColor` | `string` | Default `#000` |
| `children` | `ReactNode` | Your overlay controls, drawn above the video |

### Helpers

`formatVlcTime(ms)` returns `m:ss` or `h:mm:ss`.

## Recipes

### Fullscreen with rotation

The modal allows every orientation. Locking or rotating the screen is your app's decision, for
example with `expo-screen-orientation`:

```tsx
import * as ScreenOrientation from 'expo-screen-orientation';

const enter = async () => {
  setFullscreen(true);
  await ScreenOrientation.lockAsync(ScreenOrientation.OrientationLock.LANDSCAPE);
};
const exit = async () => {
  setFullscreen(false);
  await ScreenOrientation.lockAsync(ScreenOrientation.OrientationLock.PORTRAIT_UP);
};

<VlcFullscreenModal player={player} visible={fullscreen} onClose={exit}>
  <MyControls onExit={exit} />
</VlcFullscreenModal>;
```

### Audio and subtitle tracks

```tsx
const [tracks, setTracks] = useState<VlcTracksInfo>();
useVlcPlayerEvent(player, 'tracksChange', setTracks);

tracks?.subtitleTracks.map((track) => (
  <Button key={track.id} title={track.name} onPress={() => player.setSubtitleTrack(track.id)} />
));
```

### External subtitles and sync

```ts
player.addSubtitle('https://example.com/movie.en.srt', true);
player.subtitleDelay = 250;
player.audioDelay = -100;
```

### Snapshot

```tsx
const uri = await player.snapshot();
<Image source={{ uri }} style={{ width: 160, height: 90 }} />;
```

### Network tuning

```ts
const player = useVlcPlayer({
  uri: 'rtsp://camera.local/stream',
  vlcOptions: [':network-caching=300', ':rtsp-tcp'],
});
```

## Limitations

- **HTTP headers.** libvlc 3 can send only a user agent and a referrer. Arbitrary headers such as
  `Authorization` are not supported; use signed URLs instead.
- **`autoPlay = false`.** The media is prepared but stays `idle`, and `duration` and tracks are known
  only after `play()`.
- **Buffering progress.** There is no buffering percentage, because iOS does not report one. Use
  `status === 'buffering'`.
- **Background audio, lock screen and notification controls** are planned for 1.1 (see
  [amerllica/rn-vlc-plyr#2](https://github.com/amerllica/rn-vlc-plyr/issues/2)).
- **Picture-in-Picture and casting** are not supported.

## Troubleshooting

| Problem | Fix |
|---|---|
| `pod install` fails with `Unicode Normalization not appropriate for ASCII-8BIT` | `export LANG=en_US.UTF-8 LC_ALL=en_US.UTF-8` before running it |
| Gradle fails with `Unsupported class file major version` | Run Gradle with JDK 17 or 21 (`JAVA_HOME`) |
| `Nitro: Tried to create VlcPlayer but it does not exist` | Rebuild the native app after installing. Expo Go cannot load native code; use a development build |
| Android app is too large | Use app bundles or `abiFilters` (see [App size on Android](#app-size-on-android)) |

## Project documents

- [Behaviour contract](docs/player-contract.md): exact behaviour shared by iOS and Android.
- [Parity report](docs/parity.md): results of the cross-platform checklist.
- [Architecture decisions](docs/adr/README.md).
- [Contributing](CONTRIBUTING.md) and [code of conduct](CODE_OF_CONDUCT.md).

## Licensing

`rn-vlc-plyr` is MIT licensed.

It links against VLC: [MobileVLCKit](https://code.videolan.org/videolan/VLCKit) on iOS and
[libvlc-android](https://code.videolan.org/videolan/vlc-android) on Android. Both are licensed under
the **LGPL 2.1 or later**. Apps that ship this library also distribute those libraries and must meet
the LGPL terms, such as giving credit and letting users obtain and replace the LGPL parts. Review
the obligations for your distribution channel, including the app stores, before you ship.
