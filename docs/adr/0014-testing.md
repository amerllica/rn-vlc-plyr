# 0014. Testing strategy

Status: accepted (2026-10-07)

## Context

Video playback cannot be tested in Jest.

## Decision

Jest covers the JS layer with a fake player. XCTest and JUnit cover the pure contract rules on each OS, plus integration tests that play a real file. A manual parity checklist is run on a simulator and an emulator for each milestone.

## Consequences

Contract rules live in VLC-free types on both OSes, so they are testable and can be compared side by side.
