# 0012. Release pipeline

Status: accepted (2026-10-07)

## Context

The old setup had two publishers that could race.

## Decision

Publishing happens only in GitHub Actions on a version tag, after lint, tests and example builds pass, with npm trusted publishing (OIDC) and provenance. Tags matching `-beta.N` publish under the `beta` dist-tag.

## Consequences

No npm token is stored anywhere. Releases are created by bumping `package.json`, committing, tagging and pushing.
