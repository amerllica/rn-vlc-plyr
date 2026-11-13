const { withRnVlcPlyr } = require('./plugin/mutators');

const loadConfigPlugins = () =>
  require(
    require.resolve('expo/config-plugins', {
      paths: [process.cwd(), __dirname],
    })
  );

module.exports = (config) => withRnVlcPlyr(config, loadConfigPlugins());
