# rn-vlc-plyr

<div align="center">
  <p><strong>React Native VLC Player (Video/Audio Player) based on VLC</strong></p>
  
  [![npm version](https://img.shields.io/npm/v/rn-vlc-plyr.svg)](https://www.npmjs.com/package/rn-vlc-plyr)
  [![License](https://img.shields.io/npm/l/rn-vlc-plyr.svg)](https://github.com/amerllica/rn-vlc-plyr/blob/main/LICENSE)
  [![Platform](https://img.shields.io/badge/platform-ios%20%7C%20android-blue.svg)](https://github.com/amerllica/rn-vlc-plyr)
  [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/amerllica/rn-vlc-plyr#contributing)
  [![React Native](https://img.shields.io/badge/React_Native-Fabric_Architecture_Compatible-brightgreen)](https://reactnative.dev/docs/new-architecture-intro)
  [![Hermes](https://img.shields.io/badge/Hermes-Supported-yellow)](https://hermesengine.dev/)
</div>

<br />

A powerful and flexible React Native video/audio player library built on top of **VLC Media Player** engine. This library provides seamless video playback capabilities with support for numerous formats, streaming protocols, and advanced playback controls. Built with **React Native's New Architecture** (Fabric) and **Hermes** engine compatibility in mind.

## ✨ Features

- 🎥 **High-quality video/audio playback** powered by VLC engine
- 📱 **Cross-platform** - Works seamlessly on both iOS and Android
- 🏗️ **New Architecture Ready** - Compatible with React Native's Fabric architecture
- 🔧 **Hermes Compatible** - Optimized for Hermes JavaScript engine
- 🎯 **Rich API** - Comprehensive set of playback controls and event handlers
- 📦 **Lightweight** - Minimal footprint and efficient resource usage
- 🛠️ **Native Performance** - Direct native implementation for optimal performance
- 📺 **Fullscreen Support** - Native fullscreen controls and presentation
- 🔄 **Auto-play & Loop** - Configurable playback options
- 🔊 **Volume Control** - Dynamic volume adjustment during playback

## 📋 Requirements

- React Native >= 0.72.0 (for New Architecture/Fabric compatibility)
- iOS >= 11.0
- Android API Level >= 21 (Android 5.0+)
- Hermes enabled (recommended) or any JavaScript engine

> **⚠️ Important:** This library is designed to work with React Native's **New Architecture** (Fabric) and is **Hermes-compatible**. If you're using the legacy architecture, please consider upgrading to take advantage of the improved performance and capabilities.

## 📦 Installation

```sh
npm install rn-vlc-plyr
```

Or with yarn:

```sh
yarn add rn-vlc-plyr
```

### iOS Setup

After installing the package, navigate to your iOS directory and run:

```sh
cd ios && pod install && cd ..
```

> **Note:** This library uses MobileVLCKit (version ~3.7.0) for iOS. The podspec automatically handles the dependency.

### Android Setup

No additional setup required for Android. The library automatically integrates with your Android project.

## 🚀 Basic Usage

```jsx
import React from 'react';
import { View, StyleSheet } from 'react-native';
import { RnVlcPlyr } from 'rn-vlc-plyr';

const VideoPlayer = () => {
  const videoUrl = 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4';

  return (
    <View style={styles.container}>
      <RnVlcPlyr
        style={styles.videoPlayer}
        url={videoUrl}
        autoPlay={true}
        loop={false}
        muted={false}
        onStateChange={(event) => {
          console.log('Player state:', event.nativeEvent.state);
        }}
        onProgress={(event) => {
          console.log('Progress:', event.nativeEvent.currentTime, '/', event.nativeEvent.duration);
        }}
        onLoad={(event) => {
          console.log('Video loaded:', {
            duration: event.nativeEvent.duration,
            width: event.nativeEvent.width,
            height: event.nativeEvent.height
          });
        }}
        onError={(event) => {
          console.log('Error:', event.nativeEvent.message, 'Code:', event.nativeEvent.code);
        }}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
  },
  videoPlayer: {
    width: '100%',
    height: 250,
  },
});

export default VideoPlayer;
```

## 🎮 Advanced Usage with Controls

```jsx
import React, { useRef, useState } from 'react';
import { View, StyleSheet, Button } from 'react-native';
import { RnVlcPlyr } from 'rn-vlc-plyr';

const AdvancedVideoPlayer = () => {
  const videoRef = useRef(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isMuted, setIsMuted] = useState(false);

  const handlePlay = () => {
    videoRef.current?.play();
    setIsPlaying(true);
  };

  const handlePause = () => {
    videoRef.current?.pause();
    setIsPlaying(false);
  };

  const handleStop = () => {
    videoRef.current?.stop();
    setIsPlaying(false);
  };

  const handleSeek = (time) => {
    // Seek to 30 seconds
    videoRef.current?.seek(time);
  };

  const handleToggleMute = () => {
    setIsMuted(!isMuted);
  };

  const handleFullscreen = () => {
    videoRef.current?.presentFullscreen();
  };

  const videoUrl = 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4';

  return (
    <View style={styles.container}>
      <RnVlcPlyr
        ref={videoRef}
        style={styles.videoPlayer}
        url={videoUrl}
        autoPlay={false}
        loop={false}
        muted={isMuted}
        onStateChange={(event) => {
          const state = event.nativeEvent.state;
          console.log('Player state changed:', state);
          if (state === 'playing') {
            setIsPlaying(true);
          } else if (state === 'paused' || state === 'stopped') {
            setIsPlaying(false);
          }
        }}
      />

      <View style={styles.controls}>
        <Button title="Play" onPress={handlePlay} disabled={isPlaying} />
        <Button title="Pause" onPress={handlePause} disabled={!isPlaying} />
        <Button title="Stop" onPress={handleStop} />
        <Button title={isMuted ? "Unmute" : "Mute"} onPress={handleToggleMute} />
        <Button title="Seek 30s" onPress={() => handleSeek(30)} />
        <Button title="Fullscreen" onPress={handleFullscreen} />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
  },
  videoPlayer: {
    width: '100%',
    height: 250,
  },
  controls: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-around',
    marginTop: 20,
  },
});

export default AdvancedVideoPlayer;
```

## 📖 API Reference

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `url` | `string` | - | URL of the video/audio to play (required) |
| `autoPlay` | `boolean` | `false` | Whether to start playing automatically when loaded |
| `loop` | `boolean` | `false` | Whether to loop the video/audio |
| `muted` | `boolean` | `false` | Whether to mute the audio |
| `onStateChange` | `(event) => void` | - | Called when player state changes (buffering, playing, paused, stopped, ended, error, idle) |
| `onProgress` | `(event) => void` | - | Called periodically during playback with current time and duration |
| `onLoad` | `(event) => void` | - | Called when video is loaded with duration and dimensions |
| `onError` | `(event) => void` | - | Called when an error occurs during playback |
| `onVolumeChange` | `(event) => void` | - | Called when volume changes |

### Methods

Access these methods using a ref:

```jsx
const videoRef = useRef(null);

// Play the video
videoRef.current?.play();

// Pause the video
videoRef.current?.pause();

// Stop the video
videoRef.current?.stop();

// Seek to a specific time (in seconds)
videoRef.current?.seek(30); // Seeks to 30 seconds

// Set volume (0.0 to 1.0)
videoRef.current?.setVolume(0.5); // Sets volume to 50%

// Present fullscreen
videoRef.current?.presentFullscreen();

// Dismiss fullscreen
videoRef.current?.dismissFullscreen();
```

### Player States

The `onStateChange` callback returns one of the following states:

- `'buffering'` - Player is buffering content
- `'playing'` - Player is actively playing
- `'paused'` - Player is paused
- `'stopped'` - Player is stopped
- `'ended'` - Playback has ended
- `'error'` - An error occurred
- `'idle'` - Player is idle

## 🔧 Configuration Tips

### For iOS

- The library uses MobileVLCKit version ~3.7.0 which supports most video/audio formats
- Make sure you have properly configured your app's privacy permissions if needed
- For local files, use `file://` protocol

### For Android

- The library automatically handles all necessary configurations
- Supports most common video/audio formats through VLC's engine
- For local files, use `file:///android_asset/` or `file://` protocol

## ⚠️ Compatibility Notes

### New Architecture (Fabric) Support

This library is built with **React Native's New Architecture** (Fabric) in mind and is fully compatible. The native modules are implemented using the new codegen system.

### Hermes Engine

The library is tested and compatible with **Hermes JavaScript engine**, which is recommended for optimal performance.

> **Note:** If you're using the legacy architecture, you might experience limited functionality. We strongly recommend upgrading to React Native 0.72+ with New Architecture enabled.

## 🐛 Troubleshooting

### iOS Issues

- **Build fails with MobileVLCKit errors**: Make sure you ran `pod install` in the `ios` directory
- **Missing symbols**: Clean build folder in Xcode (`Product` → `Clean Build Folder`)
- **App crashes on startup**: Check that you're using a supported iOS version (11.0+)

### Android Issues

- **Gradle sync fails**: Make sure your project meets the minimum requirements
- **Video not playing**: Check network permissions in `android/app/src/main/AndroidManifest.xml`:
  ```xml
  <uses-permission android:name="android.permission.INTERNET" />
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
  ```
- **Build fails**: Clean and rebuild your project:
  ```sh
  cd android && ./gradlew clean && cd ..
  ```

### General Issues

- **Video not loading**: Verify the URL is accessible and uses a supported protocol (HTTP/HTTPS/RTSP/MMS, etc.)
- **Poor performance**: Consider optimizing your video content for mobile playback
- **Memory usage**: The VLC engine handles memory efficiently, but playing multiple videos simultaneously may increase usage

## 🤝 Contributing

We welcome contributions! Check out the [contributing guide](CONTRIBUTING.md) to get started.

## 📄 License

MIT

---

<div align="center">
  Made with ❤️ by <a href="https://github.com/amerllica">amerllica</a> using <a href="https://github.com/callstack/react-native-builder-bob">react-native-builder-bob</a>
</div>