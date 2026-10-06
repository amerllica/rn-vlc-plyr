# 0008. Player volume only

Status: accepted (2026-10-07)

## Context

The old code tracked system volume with fragile tricks that differ per OS.

## Decision

`volume` is the player's software gain 0 to 100 and `muted` is a separate flag. System volume is out of scope.

## Consequences

Identical on both OSes. Apps that need system volume use a dedicated library.
