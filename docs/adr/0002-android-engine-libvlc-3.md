# 0002. Android engine: libvlc-android 3.7

Status: accepted (2026-10-07)

## Context

Same need on Android. Options were libvlc-all 3.7.7 (stable), libvlc 4 (early access) and Media3 ExoPlayer.

## Decision

Use `org.videolan.android:libvlc-all:3.7.7` with one process-wide `LibVLC` instance.

## Consequences

Same libvlc 3 line as iOS, so behaviour matches. Adds about 25 MB per ABI; the README documents `abiFilters`. ExoPlayer was rejected because codec support depends on the device.
