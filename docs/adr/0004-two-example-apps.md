# 0004. Two example apps sharing one UI

Status: accepted (2026-10-07)

## Context

The library must work in bare React Native and in Expo.

## Decision

Keep `example` (bare RN, latest version) and `example-expo` (Expo SDK, prebuild and dev client). Both render `ExampleApp` from the `example-shared` workspace package.

## Consequences

Two React Native versions are exercised for free. One UI to maintain.
