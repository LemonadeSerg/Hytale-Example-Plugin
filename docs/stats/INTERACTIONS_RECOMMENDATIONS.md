# Interaction Recommendations for Stats-Driven Gameplay

This guide summarizes practical interaction patterns based on:

- Hytale interaction reference (`StatsCondition`, `ChangeStat`, flow-control interactions).
- Server interaction module source layout under `com.hypixel.hytale.server.core.modules.interaction`.

## High-value interaction patterns

### 1) Pass/fail stat gate for abilities

Use `StatsCondition` as the first step in a chain and route success/failure explicitly.

- **Pass path**: execute ability behavior and then consume resource with `ChangeStat`.
- **Fail path**: `SendMessage` and optionally a short feedback effect/sound.

Recommended sequence:

1. `StatsCondition` (`LessThan=false`, required cost threshold).
2. `Next` -> ability interactions (damage, projectile, block change, etc.).
3. Ability tail -> `ChangeStat` (negative modifier to spend resource).
4. `Failed` -> `SendMessage` (insufficient resource).

Why this pattern works:

- Keeps all affordability decisions in one interaction node.
- Makes the fail branch explicit and testable.
- Avoids desync-prone custom checks in plugin code for standard stat costs.

### 2) Cooldown + resource gating

Stack `CooldownCondition` and `StatsCondition` near the start of the chain.

- Use cooldown as the first coarse gate to avoid expensive logic when locked.
- Follow with `StatsCondition` for resource availability.
- On success, trigger the ability and then `TriggerCooldown` / `ResetCooldown` as needed.

### 3) Charge-based skills with fork actions

Use `Charging` where hold duration matters.

- Map hold-time thresholds in `Charging.Next` for weak/medium/strong outcomes.
- Use `Forks` to allow alternate actions while charging (for example, defensive fork).
- Add explicit cooldown padding when needed to prevent immediate re-trigger loops.

### 4) Selector fan-out for AOE effects

Use `Selector` to fork per target and keep root logic readable.

- Configure `FailOn` intentionally (`Neither`, `Entity`, `Block`, `Either`).
- Keep per-target logic in the forked `RootInteraction`.
- Combine with stat/cooldown gates before selector execution.

## Suggested baseline templates

### Template A: Resource-costed combat skill

- `CooldownCondition`
- `StatsCondition`
- `DamageEntity` / `Projectile`
- `ChangeStat` (consume)
- `TriggerCooldown`
- `Failed` branch: `SendMessage`

### Template B: Utility action with soft failure

- `StatsCondition`
- `ChangeBlock` / `UseBlock` / `ModifyInventory`
- `ChangeStat`
- `Failed` branch: `SendMessage` (or no-op if silent failure is preferred)

## Plugin integration guidance

For external plugins that react to stat updates:

- Use `onStatsPreUpdate` when you need before-state snapshots.
- Use `onStatsUpdated` when you need persisted after-state values.
- Keep listeners lightweight and queue heavy work out-of-band.

## Custom stats interaction

This plugin exposes a stats-focused custom interaction type: `LemStatCondition`.

Behavior:

- Reads a player stat from `PlayerStatsComponent`.
- Checks `MinimumOccurrences` and `MinimumQuantity`.
- Marks interaction as `Finished` when thresholds pass, otherwise `Failed`.

Fields:

- `StatType` (required): one of `GameEventType` names, e.g. `BLOCK_BREAK`.
- `PrimaryId` (optional): if provided, checks the stat bucket for that id; otherwise checks event totals.
- `MinimumOccurrences` (optional, default `1`).
- `MinimumQuantity` (optional, default `0`).

Example:

```json
{
	"Type": "LemStatCondition",
	"StatType": "BLOCK_BREAK",
	"PrimaryId": "BlockTypes_Stone",
	"MinimumOccurrences": 50,
	"MinimumQuantity": 50,
	"Next": "Interactions/RewardStoneBreaker",
	"Failed": "Interactions/NeedMoreStone"
}
```

## Cautions

- `UseEntity` is documented as unusual and generally not recommended.
- `AddItem` has known caveats; prefer `ModifyInventory` for reliability.
- Shared cooldown IDs should use compatible cooldown configurations to avoid cross-ability side effects.