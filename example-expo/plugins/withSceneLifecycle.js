const { withAppDelegate, withInfoPlist } = require('expo/config-plugins');

const APP_DELEGATE_DECLARATION = 'class AppDelegate: ExpoAppDelegate {';
const SCENE_AWARE_DECLARATION =
  'class AppDelegate: ExpoAppDelegate, ExpoReactNativeFactoryProvider {';
const WINDOW_STARTUP =
  /#if os\(iOS\) \|\| os\(tvOS\)\s*window = UIWindow\(frame: UIScreen\.main\.bounds\)\s*factory\.startReactNative\([\s\S]*?\)\s*#endif\s*/;
const SCENE_DELEGATE = '\nclass SceneDelegate: ExpoAppSceneDelegate {}\n';

const SCENE_MANIFEST = {
  UIApplicationSupportsMultipleScenes: false,
  UISceneConfigurations: {
    UIWindowSceneSessionRoleApplication: [
      {
        UISceneConfigurationName: 'Default Configuration',
        UISceneDelegateClassName: '$(PRODUCT_MODULE_NAME).SceneDelegate',
      },
    ],
  },
};

const adoptSceneLifecycle = (contents) => {
  if (contents.includes('ExpoAppSceneDelegate')) {
    return contents;
  }
  if (
    !contents.includes(APP_DELEGATE_DECLARATION) ||
    !WINDOW_STARTUP.test(contents)
  ) {
    throw new Error(
      'withSceneLifecycle: unexpected AppDelegate.swift template'
    );
  }
  return (
    contents
      .replace(APP_DELEGATE_DECLARATION, SCENE_AWARE_DECLARATION)
      .replace(WINDOW_STARTUP, '') + SCENE_DELEGATE
  );
};

module.exports = function withSceneLifecycle(config) {
  const withDelegate = withAppDelegate(config, (mod) => {
    mod.modResults.contents = adoptSceneLifecycle(mod.modResults.contents);
    return mod;
  });
  return withInfoPlist(withDelegate, (mod) => {
    mod.modResults.UIApplicationSceneManifest = SCENE_MANIFEST;
    return mod;
  });
};
