# Milestones Modding Event API

Use `MilestonesEvents` to subscribe to Milestones handler events from another plugin.

## Registering listeners

```java
import com.lemonadesergeant.milestones.api.MilestonesEvents;
import com.lemonadesergeant.milestones.listeners.SystemEventListeners;

public final class MyPluginHooks {

    private SystemEventListeners.Registration blockBreakRegistration;

    public void register() {
        blockBreakRegistration = MilestonesEvents.onBlockBreak(payload -> {
            // Relevant curated fields:
            // payload.playerDisplayName
            // payload.blockTypeId
            // payload.itemInHandId
            // payload.targetBlock
        });
    }

    public void unregister() {
        if (blockBreakRegistration != null) {
            blockBreakRegistration.unregister();
            blockBreakRegistration = null;
        }
    }
}
```

## Available subscriptions

- `MilestonesEvents.onBlockBreak(...)`
- `MilestonesEvents.onBlockPlace(...)`
- `MilestonesEvents.onBlockUse(...)`
- `MilestonesEvents.onCraftRecipe(...)`
- `MilestonesEvents.onDealDamage(...)` (from player only)
- `MilestonesEvents.onDealtDamage(...)` (to player only)
- `MilestonesEvents.onKill(...)` (entities killed by player only)
- `MilestonesEvents.onKilled(...)` (player killed only)
- `MilestonesEvents.onInteractivelyPickup(...)`
- `MilestonesEvents.onPickup(...)`
- `MilestonesEvents.onZoneDiscovery(...)`
- `MilestonesEvents.onBiomeDiscovery(...)`

Legacy broad events are still available if you need all combat events:

- `MilestonesEvents.onDamage(...)`
- `MilestonesEvents.onEntityKill(...)`

## Combat listener payload quick reference

- `onDealDamage`: source player info + target ref + amount/cause + cancellation state.
- `onDealtDamage`: target player info + source ref + amount/cause + cancellation state.
- `onKill`: killer player info + target ref + kill cause + amount.
- `onKilled`: victim player info + killer ref + kill cause + amount.

Each callback receives a typed payload class from `SystemEventListeners` with curated fields.

## Notes for external modders

- Your mod/plugin must have Milestones on its compile and runtime classpath.
- Register listeners after Milestones has loaded.
- Keep listener logic lightweight; expensive work should be queued or deferred.

## Stats API accessors and update hooks

Use `MilestonesStatsApi` to read persisted stats and subscribe to pre/post update notifications.

```java
import com.lemonadesergeant.milestones.api.ListenerRegistration;
import com.lemonadesergeant.milestones.api.MilestonesStatsApi;
import com.lemonadesergeant.milestones.api.StatsPreUpdate;
import com.lemonadesergeant.milestones.api.StatsUpdate;
import com.lemonadesergeant.milestones.data.GameEventType;

public final class MyStatsHooks {

    private ListenerRegistration statsPreRegistration;
    private ListenerRegistration statsPostRegistration;

    public void register() {
        statsPreRegistration = MilestonesStatsApi.onStatsPreUpdate(this::onStatsPreUpdate);
        statsPostRegistration = MilestonesStatsApi.onStatsUpdated(this::onStatsUpdated);
    }

    private void onStatsPreUpdate(StatsPreUpdate update) {
        if (update.getEventType() == GameEventType.BLOCK_BREAK) {
            long before = update.getTotalOccurrencesBefore();
            String blockId = update.getPrimaryValue();
        }
    }

    private void onStatsUpdated(StatsUpdate update) {
        if (update.getEventType() == GameEventType.BLOCK_BREAK) {
            long after = update.getTotalOccurrencesAfter();
            String blockId = update.getPrimaryValue();
        }
    }

    public void unregister() {
        if (statsPreRegistration != null) {
            statsPreRegistration.unregister();
            statsPreRegistration = null;
        }

        if (statsPostRegistration != null) {
            statsPostRegistration.unregister();
            statsPostRegistration = null;
        }
    }
}
```

Available accessors:

- `MilestonesStatsApi.getPlayerStats(store, playerRef)`
- `MilestonesStatsApi.getTotalOccurrences(store, playerRef, eventType)`
- `MilestonesStatsApi.getTotalQuantity(store, playerRef, eventType)`
- `MilestonesStatsApi.getPrimaryOccurrences(store, playerRef, eventType)`
- `MilestonesStatsApi.onStatsPreUpdate(listener)`
- `MilestonesStatsApi.onStatsUpdated(listener)`

Related docs split:

- Stats docs: `docs/stats/README.md`
- Milestones docs: `docs/milestones/README.md`
