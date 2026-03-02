# Stats Documentation

This section documents the implemented stats data model, plugin API, and integration patterns.

## Public API

Use `MilestonesStatsApi` for querying persisted player stats and subscribing to stat-change callbacks.

- `MilestonesStatsApi.getPlayerStats(store, playerRef)`
- `MilestonesStatsApi.getTotalOccurrences(store, playerRef, eventType)`
- `MilestonesStatsApi.getTotalQuantity(store, playerRef, eventType)`
- `MilestonesStatsApi.getPrimaryOccurrences(store, playerRef, eventType)`
- `MilestonesStatsApi.onStatsPreUpdate(listener)`
- `MilestonesStatsApi.onStatsUpdated(listener)`

## Pre and post update hooks

The stats manager now exposes two read-only lifecycle hooks:

- **Pre-update** (`StatsPreUpdateListener`): called before the event mutates stored stats.
- **Post-update** (`StatsUpdateListener`): called after the event is applied and persisted.

Example:

```java
import com.lemonadesergeant.milestones.api.ListenerRegistration;
import com.lemonadesergeant.milestones.api.MilestonesStatsApi;
import com.lemonadesergeant.milestones.api.StatsPreUpdate;
import com.lemonadesergeant.milestones.api.StatsUpdate;

public final class MyStatsHooks {

    private ListenerRegistration preRegistration;
    private ListenerRegistration postRegistration;

    public void register() {
        preRegistration = MilestonesStatsApi.onStatsPreUpdate(this::onStatsPreUpdate);
        postRegistration = MilestonesStatsApi.onStatsUpdated(this::onStatsUpdated);
    }

    private void onStatsPreUpdate(StatsPreUpdate update) {
        // Read current totals before the write.
        long before = update.getTotalOccurrencesBefore();
    }

    private void onStatsUpdated(StatsUpdate update) {
        // Read totals after the write.
        long after = update.getTotalOccurrencesAfter();
    }

    public void unregister() {
        if (preRegistration != null) {
            preRegistration.unregister();
            preRegistration = null;
        }

        if (postRegistration != null) {
            postRegistration.unregister();
            postRegistration = null;
        }
    }
}
```

## Additional stats docs

- See `docs/reference/MODDING_EVENT_API.md` for event subscription and API quick reference.
- See `docs/stats/INTERACTIONS_RECOMMENDATIONS.md` for interaction-chain guidance (including pass/fail stat checks).