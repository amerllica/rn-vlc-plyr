# 0021. Release notes from the changelog

Status: accepted (2026-10-22)

## Context

The release job used `gh release create --generate-notes`. GitHub builds those notes from merged
pull requests. Changes land on `main` as commits without pull requests, so every GitHub release
had only a compare link. The commit subjects are not a good release body either: many are `docs:`,
`chore:` and `test:` steps that users do not need.

## Decision

- `CHANGELOG.md` at the repository root, in the Keep a Changelog format, is written by hand. Its
  section for a version is the body of that GitHub release.
- `scripts/release-notes.js` takes the section, removes empty subsections, turns relative links into
  links to the tagged files, and adds the install command, the npm link, the native engine versions
  read from the podspec, Gradle and `package.json`, and the full changelog link.
- The release job builds the notes before it publishes to npm. A stable version without a section
  stops the release. A beta without its own section uses the `Unreleased` section.
- The lint job runs the same script for the `package.json` version on every push and pull request.

## Consequences

Every version bump also edits `CHANGELOG.md`. The commit history stays the raw material for a
section, not the section itself (see [ADR 0015](0015-commits.md)). npm users can read the changelog
in the package repository.
