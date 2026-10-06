# 0003. Bridge: Nitro Modules

Status: accepted (2026-10-07)

## Context

The library needs Swift and Kotlin, JSI speed and no Expo dependency for bare apps. Options were Nitro, Fabric codegen and the Expo Modules API.

## Decision

Scaffold with `create-react-native-library --type nitro-view --languages kotlin-swift` and use `react-native-nitro-modules` as a peer dependency.

## Consequences

Pure Swift and Kotlin, typed from one TypeScript spec, synchronous getters. Nitro is pre-1.0, so its version is tracked closely. Requires the new architecture and RN 0.78 or newer.
