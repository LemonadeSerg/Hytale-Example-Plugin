# Development Guide

Development notes for Milestones 2.0 internals.

## Stats Persistence Architecture

Stats are stored per player in `PlayerStatsComponent` and persisted by the engine through the entity component system.

### Runtime write-safety rule

Hytale `Store` rejects component writes while ECS systems are processing. This means event systems must never call mutating store APIs directly during processing.

Current implementation in `StatsManager`:

- `recordEvent(...)` checks `store.isProcessing()`.
- If processing is active, events are queued in a per-player deferred queue.
- Only one flush task is scheduled per player at a time.
- The world task queue flushes deferred events and applies component writes when writes are safe.

This prevents world-thread crashes like:

- `IllegalStateException: Store is currently processing! Ensure you aren't calling a store method from a system.`

### Current routing scope (stats-only)

Milestone forwarding is intentionally disabled right now. `EventForwardingManager` forwards only to stats code paths.

- Active: `StatsManager` and `StatsProgressHelper`
- Inactive: `MilestoneManager` / milestone progress routing

### Log tag format in stats flow

For stats runtime logs, the first tag is the originating event source and the second tag identifies the manager/helper stage.

Example:

- `[BLOCK_BREAK] [STATS_MANAGER] ...`
- `[BLOCK_BREAK] [STATS_HELPER] ...`

### Key classes

- `StatsManager`: write safety, deferred queueing, and component mutation.
- `StatsProgressHelper`: maps normalized events to stats dimensions/counters.
- `PlayerStatsData`: serializable aggregate model for totals, primary IDs, and split dimensions.
- `PlayerStatsComponent`: serialized component wrapper persisted per player.

## Command Surface

Root command collection:

- `/milestones` (alias `/ms`)

Stats commands:

- `/milestones stats [--player <player>]`
- `/milestones stats --statType <type> [--player <player>]`
- `/milestones stats --statType <type> --id <id> [--player <player>]`
- `/milestones stats types`

Notes:

- `--player` is optional and defaults to the sender (via `AbstractTargetPlayerCommand`).
- `--id` requires `--statType`.

## Validation Workflow

- Compile check: `./gradlew compileJava`
- Runtime smoke: `./gradlew runServer`
- Regression target: join world and trigger pickup/break events, confirm no store-processing write exception.
