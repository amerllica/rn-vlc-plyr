# 0011. Restart on an orphan branch

Status: accepted (2026-10-07)

## Context

The previous code base had structural bugs and an outdated scaffold.

## Decision

Start an orphan branch `rewrite` from a fresh scaffold. Keep the old history at branch `legacy` and tag `legacy-v0.2.0`. Replace `main` with `rewrite` after review.

## Consequences

Clean history; nothing lost.
