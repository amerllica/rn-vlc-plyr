const BACKGROUND_AUDIO_MODE = 'audio';
const PLAYBACK_SERVICE = 'com.margelo.nitro.rnvlcplyr.VlcPlaybackService';
const PLAYBACK_PERMISSIONS = [
  'android.permission.FOREGROUND_SERVICE',
  'android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK',
];

const withBackgroundAudioMode = (infoPlist) => {
  const modes = infoPlist.UIBackgroundModes ?? [];
  if (modes.includes(BACKGROUND_AUDIO_MODE)) {
    return infoPlist;
  }
  return { ...infoPlist, UIBackgroundModes: [...modes, BACKGROUND_AUDIO_MODE] };
};

const namedEntry = (name, attributes = {}) => ({
  $: { 'android:name': name, ...attributes },
});

const hasNamedEntry = (entries, name) =>
  entries.some((entry) => entry.$?.['android:name'] === name);

const appendMissing = (entries, name, attributes) =>
  hasNamedEntry(entries, name)
    ? entries
    : [...entries, namedEntry(name, attributes)];

const withPlaybackService = (application) => ({
  ...application,
  service: appendMissing(application.service ?? [], PLAYBACK_SERVICE, {
    'android:exported': 'false',
    'android:foregroundServiceType': 'mediaPlayback',
  }),
});

const withPlaybackManifest = (androidManifest) => {
  const manifest = androidManifest.manifest;
  const permissions = PLAYBACK_PERMISSIONS.reduce(
    (entries, permission) => appendMissing(entries, permission),
    manifest['uses-permission'] ?? []
  );
  const [application = { $: {} }, ...otherApplications] =
    manifest.application ?? [];
  return {
    ...androidManifest,
    manifest: {
      ...manifest,
      'uses-permission': permissions,
      'application': [withPlaybackService(application), ...otherApplications],
    },
  };
};

module.exports = {
  BACKGROUND_AUDIO_MODE,
  PLAYBACK_PERMISSIONS,
  PLAYBACK_SERVICE,
  withBackgroundAudioMode,
  withPlaybackManifest,
};
