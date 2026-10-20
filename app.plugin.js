const {
  withBackgroundAudioMode,
  withPlaybackManifest,
} = require('./plugin/mutators');

const loadConfigPlugins = () =>
  require(
    require.resolve('expo/config-plugins', {
      paths: [process.cwd(), __dirname],
    })
  );

module.exports = function withRnVlcPlyr(config) {
  const { withAndroidManifest, withInfoPlist } = loadConfigPlugins();
  const withIos = withInfoPlist(config, (mod) => {
    mod.modResults = withBackgroundAudioMode(mod.modResults);
    return mod;
  });
  return withAndroidManifest(withIos, (mod) => {
    mod.modResults = withPlaybackManifest(mod.modResults);
    return mod;
  });
};
