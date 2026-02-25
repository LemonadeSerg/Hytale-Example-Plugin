# AI Workflow Guidance

This document defines the baseline workflow for AI-assisted and human-assisted changes in this repository.

## Goals

- Keep commits consistent and easy to review.
- Keep documentation aligned with code changes.
- Keep build and runtime information current and reproducible.
- Ensure contributors know where to find upstream server source references.

## Commit Consistency

- Use focused commits with one primary purpose.
- Prefer Conventional Commit style:
  - `feat:` new behavior
  - `fix:` bug resolution
  - `refactor:` internal code cleanup without behavior changes
  - `docs:` documentation-only updates
  - `chore:` tooling, build, cleanup, maintenance
- Commit message format:
  - `type: short imperative summary`
  - Optional body with:
    - what changed
    - why it changed
    - any migration/compatibility note
- Do not mix unrelated changes in one commit.
- Before commit:
  - run build checks
  - review staged diff
  - verify docs and changelog updates when applicable

## Documentation Update Rules

Update docs in the same change when behavior, project layout, or usage changes.

- Update [README.md](../../../README.md) when:
  - setup/build steps change
  - command usage changes
  - important folder structure changes
- Update [docs/guides/DEVELOPMENT.md](DEVELOPMENT.md) when:
  - architecture or implementation approach changes
  - development workflow or standards change
- Update [docs/guides/MILESTONE_CREATION.md](MILESTONE_CREATION.md) when:
  - milestone schema/authoring guidance changes
- Update [docs/reference/API_REFERENCE.md](../reference/API_REFERENCE.md) when:
  - public classes/interfaces/contracts change
- Update [docs/release/CHANGELOG.md](../release/CHANGELOG.md) for user-visible or baseline-impacting changes.

## Build & Validation Expectations

Preferred baseline checks before commit:

1. `./gradlew clean build`
2. Resolve compile errors introduced by your change.
3. If build cannot run locally, document why in commit body or PR description.

Optional (recommended for bigger changes):

- `./gradlew tasks` (confirm expected tasks)
- any project-specific run/test commands used by maintainers

## AI Chat Working Agreement (Current and Future)

For AI-assisted updates in this repository:

- Prefer minimal, surgical edits.
- Preserve existing naming and folder conventions unless explicitly refactoring.
- Avoid speculative features outside the requested scope.
- Keep docs and code synchronized in the same change set.
- Surface blockers clearly (missing tools, missing credentials, environment issues).
- End each substantial change with a concise status summary and next action.

## Source Reference: Hytale Server Sources

When API or behavior reference is needed, use the local Hytale server source bundle:

- [HytaleServer-sources](E:/Games/Hypixel%20Studio/Hytale/install/release/package/game/latest/Server/HytaleServer-sources)

Important notes:

- This location is external to this repository and should be treated as reference material.
- Do not copy large third-party/internal source content into this repository.
- Prefer implementing against published APIs and local plugin code.
