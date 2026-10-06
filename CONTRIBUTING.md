# Contributing

Contributions are always welcome, no matter how large or small!

We want this community to be friendly and respectful to each other. Please follow it in all your interactions with the project. Before contributing, please read the [code of conduct](./CODE_OF_CONDUCT.md).

## Development workflow

This project is a monorepo managed using [Yarn workspaces](https://yarnpkg.com/features/workspaces). It contains the following packages:

- The library package in the root directory.
- `example/`: the bare React Native example app (latest React Native).
- `example-expo/`: the Expo example app (development build, prebuild).
- `example-shared/`: the demo UI that both example apps render.

Native changes must keep iOS and Android identical. The rules live in
[`docs/player-contract.md`](docs/player-contract.md); update it in the same pull request when
behaviour changes, and add native tests on both platforms.

To get started with the project, make sure you have the correct version of [Node.js](https://nodejs.org/) installed. See the [`.nvmrc`](./.nvmrc) file for the version used in this project.

Run `yarn` in the root directory to install the required dependencies for each package:

```sh
yarn
```

> Since the project relies on Yarn workspaces, you cannot use [`npm`](https://github.com/npm/cli) for development without manually migrating.

This project uses Nitro Modules. If you're not familiar with how Nitro works, make sure to check the [Nitro Modules Docs](https://nitro.margelo.com/).

You need to run [Nitrogen](https://nitro.margelo.com/docs/nitrogen) to generate the boilerplate code required for this project. The example app will not build without this step.

Run **Nitrogen** in following cases:

- When you make changes to any `*.nitro.ts` files.
- When running the project for the first time (since the generated files are not committed to the repository).

To invoke **Nitrogen**, use the following command:

```sh
yarn nitrogen
```

The [example app](/example/) demonstrates usage of the library. You need to run it to test any changes you make.

It is configured to use the local version of the library, so any changes you make to the library's source code will be reflected in the example app. Changes to the library's JavaScript code will be reflected in the example app without a rebuild, but native code changes will require a rebuild of the example app.

If you want to use Android Studio or Xcode to edit the native code, you can open the `example/android` or `example/ios` directories respectively in those editors. To edit the Objective-C or Swift files, open `example/ios/RnVlcPlyrExample.xcworkspace` in Xcode and find the source files at `Pods > Development Pods > rn-vlc-plyr`.

To edit the Java or Kotlin files, open `example/android` in Android studio and find the source files at `rn-vlc-plyr` under `Android`.

You can use various commands from the root directory to work with the project.

To start the packager:

```sh
yarn example start
```

To run the example app on Android:

```sh
yarn example android
```

To run the example app on iOS:

```sh
yarn example ios
```

To confirm that the app is running with the new architecture, you can check the Metro logs for a message like this:

```sh
Running "RnVlcPlyrExample" with {"fabric":true,"initialProps":{"concurrentRoot":true},"rootTag":1}
```

Note the `"fabric":true` and `"concurrentRoot":true` properties.

To run the Expo example app:

```sh
yarn example:expo prebuild
yarn example:expo ios
yarn example:expo android
```

Native builds need two environment settings on macOS:

```sh
export LANG=en_US.UTF-8 LC_ALL=en_US.UTF-8
export JAVA_HOME=/path/to/jdk-21
```

Run the native tests:

```sh
cd example/android && ./gradlew :rn-vlc-plyr:testDebugUnitTest :rn-vlc-plyr:connectedDebugAndroidTest
xcodebuild test -workspace example/ios/RnVlcPlyrExample.xcworkspace -scheme RnVlcPlyr-Unit-Tests -destination 'platform=iOS Simulator,name=iPhone 17 Pro'
```

Make sure your code passes TypeScript:

```sh
yarn typecheck
```

To check for linting errors, run the following:

```sh
yarn lint
```

To fix formatting errors, run the following:

```sh
yarn lint --fix
```

Remember to add tests for your change if possible. Run the unit tests by:

```sh
yarn test
```


### Commit message convention

We follow the [conventional commits specification](https://www.conventionalcommits.org/en) for our commit messages:

- `fix`: bug fixes, e.g. fix crash due to deprecated method.
- `feat`: new features, e.g. add new method to the module.
- `refactor`: code refactor, e.g. migrate from class components to hooks.
- `docs`: changes into documentation, e.g. add usage example for the module.
- `test`: adding or updating tests, e.g. add integration tests using detox.
- `chore`: tooling changes, e.g. change CI config.

Our pre-commit hooks verify that your commit message matches this format when committing.


### Scripts

The `package.json` file contains various scripts for common tasks:

- `yarn`: set up the project by installing dependencies.
- `yarn nitrogen`: regenerate the Nitro bindings after changing a `*.nitro.ts` file.
- `yarn typecheck`: type-check files with TypeScript.
- `yarn lint`: lint files with [ESLint](https://eslint.org/).
- `yarn test`: run unit tests with [Jest](https://jestjs.io/).
- `yarn example start`: start Metro for the bare example app.
- `yarn example android` / `yarn example ios`: run the bare example app.
- `yarn example:expo android` / `yarn example:expo ios`: run the Expo example app.

### Publishing

Releases are published by GitHub Actions, never from a laptop:

1. Set `version` in `package.json` (for example `1.0.0` or `1.0.0-beta.1`) and commit it on `main`.
2. Tag the commit with the same version and push the tag: `git tag v1.0.0 && git push origin v1.0.0`.
3. The workflow runs every check, then publishes to npm with provenance through trusted publishing.
   Versions with `-beta.N` go to the `beta` dist-tag and become a GitHub pre-release.

### Sending a pull request

> **Working on your first pull request?** You can learn how from this _free_ series: [How to Contribute to an Open Source Project on GitHub](https://app.egghead.io/playlists/how-to-contribute-to-an-open-source-project-on-github).

When you're sending a pull request:

- Prefer small pull requests focused on one change.
- Verify that linters and tests are passing.
- Review the documentation to make sure it looks good.
- Follow the pull request template when opening a pull request.
- For pull requests that change the API or implementation, discuss with maintainers first by opening an issue.
