const { describe, expect, it } = require('@jest/globals');
const {
  PLAYBACK_PERMISSIONS,
  PLAYBACK_SERVICE,
  withBackgroundAudioMode,
  withPlaybackManifest,
} = require('../mutators');

const names = (entries) => entries.map((entry) => entry.$['android:name']);

const emptyManifest = () => ({
  manifest: { $: {}, application: [{ $: { 'android:name': '.App' } }] },
});

describe('withBackgroundAudioMode', () => {
  it('adds the audio mode to an empty plist', () => {
    expect(withBackgroundAudioMode({})).toEqual({
      UIBackgroundModes: ['audio'],
    });
  });

  it('keeps existing modes and other keys', () => {
    const result = withBackgroundAudioMode({
      CFBundleName: 'App',
      UIBackgroundModes: ['fetch'],
    });

    expect(result).toEqual({
      CFBundleName: 'App',
      UIBackgroundModes: ['fetch', 'audio'],
    });
  });

  it('does not add the audio mode twice', () => {
    const once = withBackgroundAudioMode({});

    expect(withBackgroundAudioMode(once)).toBe(once);
  });
});

describe('withPlaybackManifest', () => {
  it('adds the permissions and the playback service', () => {
    const { manifest } = withPlaybackManifest(emptyManifest());
    const [application] = manifest.application;

    expect(names(manifest['uses-permission'])).toEqual(PLAYBACK_PERMISSIONS);
    expect(application.$['android:name']).toBe('.App');
    expect(application.service).toEqual([
      {
        $: {
          'android:name': PLAYBACK_SERVICE,
          'android:exported': 'false',
          'android:foregroundServiceType': 'mediaPlayback',
        },
      },
    ]);
  });

  it('keeps existing permissions and services', () => {
    const input = emptyManifest();
    input.manifest['uses-permission'] = [
      { $: { 'android:name': 'android.permission.INTERNET' } },
    ];
    input.manifest.application[0].service = [
      { $: { 'android:name': '.OtherService' } },
    ];

    const { manifest } = withPlaybackManifest(input);

    expect(names(manifest['uses-permission'])).toEqual([
      'android.permission.INTERNET',
      ...PLAYBACK_PERMISSIONS,
    ]);
    expect(names(manifest.application[0].service)).toEqual([
      '.OtherService',
      PLAYBACK_SERVICE,
    ]);
  });

  it('does not duplicate entries when applied twice', () => {
    const twice = withPlaybackManifest(withPlaybackManifest(emptyManifest()));

    expect(names(twice.manifest['uses-permission'])).toEqual(
      PLAYBACK_PERMISSIONS
    );
    expect(names(twice.manifest.application[0].service)).toEqual([
      PLAYBACK_SERVICE,
    ]);
  });
});
