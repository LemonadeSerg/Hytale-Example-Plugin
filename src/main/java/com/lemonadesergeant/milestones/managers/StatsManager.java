package com.lemonadesergeant.milestones.managers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.lemonadesergeant.milestones.api.ListenerRegistration;
import com.lemonadesergeant.milestones.api.StatsPreUpdate;
import com.lemonadesergeant.milestones.api.StatsPreUpdateListener;
import com.lemonadesergeant.milestones.api.StatsUpdate;
import com.lemonadesergeant.milestones.api.StatsUpdateListener;
import com.lemonadesergeant.milestones.MilestonesPlugin;
import com.lemonadesergeant.milestones.data.GameEventType;
import com.lemonadesergeant.milestones.data.NormalizedGameEvent;
import com.lemonadesergeant.milestones.data.PlayerStatsComponent;
import com.lemonadesergeant.milestones.data.PlayerStatsData;
import com.lemonadesergeant.milestones.logging.LogSource;
import com.lemonadesergeant.milestones.logging.LogStage;
import com.lemonadesergeant.milestones.logging.PluginLog;

/**
 * Handles stats persistence for normalized gameplay events.
 *
 * <p>Writes to {@link Store} are not allowed while ECS systems are processing.
 * To prevent world-thread crashes, this manager defers those writes into a
 * per-player queue and schedules a single flush task per player.
 */
public class StatsManager {

    private static final int MAX_DEFERRED_EVENTS_PER_PLAYER = 2048;

    private final StatsProgressHelper progressHelper = new StatsProgressHelper();
    private final Map<Ref<EntityStore>, DeferredPlayerQueue> deferredByPlayerRef = new IdentityHashMap<>();
    private final CopyOnWriteArrayList<StatsPreUpdateListener> statsPreUpdateListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<StatsUpdateListener> statsUpdateListeners = new CopyOnWriteArrayList<>();

    public StatsManager() {
        PluginLog.info(LogSource.STATS_MANAGER, LogStage.SETUP, "component=StatsManager action=init");
    }

    /**
     * Records one normalized event for the provided player.
     *
     * <p>If the store is currently processing ECS systems, the write is queued
     * and applied later on the world task queue.
     */
    public void recordEvent(Store<EntityStore> store, Ref<EntityStore> playerRef, NormalizedGameEvent event) {
        LogSource eventSource = EventLogSourceResolver.resolve(event, LogSource.STATS_MANAGER);
        PluginLog.info(
            eventSource,
            LogStage.STATS_MANAGER,
            "component=StatsManager action=recordEvent eventType=%s hasStore=%s hasPlayerRef=%s",
            event == null ? null : event.getType(),
            store != null,
            playerRef != null
        );

        if (store == null || playerRef == null || event == null || !playerRef.isValid()) {
            return;
        }

        if (store.isProcessing()) {
            enqueueDeferred(store, playerRef, event);
            return;
        }

        applyRecordNow(store, playerRef, event);
    }

    private void enqueueDeferred(Store<EntityStore> store, Ref<EntityStore> playerRef, NormalizedGameEvent event) {
        boolean scheduleFlush;

        synchronized (deferredByPlayerRef) {
            DeferredPlayerQueue queue = deferredByPlayerRef.computeIfAbsent(playerRef, ignored -> new DeferredPlayerQueue());
            queue.events.addLast(event);

            if (queue.events.size() > MAX_DEFERRED_EVENTS_PER_PLAYER) {
                queue.events.removeFirst();
                PluginLog.warn(
                    EventLogSourceResolver.resolve(event, LogSource.STATS_MANAGER),
                    LogStage.STATS_MANAGER,
                    "component=StatsManager action=recordEvent deferred=true droppedOldest=true reason=queue-cap playerRef=%s cap=%s",
                    playerRef,
                    MAX_DEFERRED_EVENTS_PER_PLAYER
                );
            }

            scheduleFlush = !queue.flushScheduled;
            if (scheduleFlush) {
                queue.flushScheduled = true;
            }
        }

        PluginLog.info(
            EventLogSourceResolver.resolve(event, LogSource.STATS_MANAGER),
            LogStage.STATS_MANAGER,
            "component=StatsManager action=recordEvent deferred=true reason=store-processing eventType=%s scheduleFlush=%s",
            event.getType(),
            scheduleFlush
        );

        if (scheduleFlush) {
            store.getExternalData().getWorld().execute(() -> flushDeferred(store, playerRef));
        }
    }

    private void flushDeferred(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        if (store == null || playerRef == null || !playerRef.isValid()) {
            synchronized (deferredByPlayerRef) {
                deferredByPlayerRef.remove(playerRef);
            }
            return;
        }

        if (store.isProcessing()) {
            store.getExternalData().getWorld().execute(() -> flushDeferred(store, playerRef));
            return;
        }

        List<NormalizedGameEvent> toApply;
        synchronized (deferredByPlayerRef) {
            DeferredPlayerQueue queue = deferredByPlayerRef.get(playerRef);
            if (queue == null || queue.events.isEmpty()) {
                if (queue != null) {
                    queue.flushScheduled = false;
                    deferredByPlayerRef.remove(playerRef);
                }
                return;
            }

            toApply = new ArrayList<>(queue.events);
            queue.events.clear();
            queue.flushScheduled = false;
            deferredByPlayerRef.remove(playerRef);
        }

        for (NormalizedGameEvent event : toApply) {
            applyRecordNow(store, playerRef, event);
        }
    }

    private void applyRecordNow(Store<EntityStore> store, Ref<EntityStore> playerRef, NormalizedGameEvent event) {
        if (store == null || playerRef == null || event == null || !playerRef.isValid()) {
            return;
        }

        if (store.isProcessing()) {
            enqueueDeferred(store, playerRef, event);
            return;
        }

        var componentType = MilestonesPlugin.instance().getPlayerStatsComponentType();
        PlayerStatsComponent statsComponent = store.getComponent(playerRef, componentType);
        if (statsComponent == null) {
            statsComponent = new PlayerStatsComponent();
        }

        PlayerStatsData playerStats = statsComponent.getStatsData();
        publishStatsPreUpdate(store, playerRef, event, playerStats);
        progressHelper.recordEvent(playerStats, event);
        store.putComponent(playerRef, componentType, statsComponent);
        publishStatsUpdated(store, playerRef, event, playerStats);
    }

    public PlayerStatsData getOrCreatePlayerStats(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        if (store == null || playerRef == null || !playerRef.isValid()) {
            return new PlayerStatsData();
        }

        var componentType = MilestonesPlugin.instance().getPlayerStatsComponentType();
        PlayerStatsComponent statsComponent = store.ensureAndGetComponent(playerRef, componentType);
        return statsComponent.getStatsData();
    }

    public long getBlockBreakTotalForBlock(Store<EntityStore> store, Ref<EntityStore> playerRef, String blockId) {
        PlayerStatsData stats = getOrCreatePlayerStats(store, playerRef);
        return stats == null ? 0 : stats.getBlockBreakTotal(blockId);
    }

    public Map<String, Long> getBlockBreakByHeldItem(Store<EntityStore> store, Ref<EntityStore> playerRef, String blockId) {
        PlayerStatsData stats = getOrCreatePlayerStats(store, playerRef);
        return stats == null ? Map.of() : stats.getBlockBreakByHeldItem(blockId);
    }

    public long getTotalEventOccurrences(Store<EntityStore> store, Ref<EntityStore> playerRef, GameEventType eventType) {
        PlayerStatsData stats = getOrCreatePlayerStats(store, playerRef);
        return stats == null ? 0 : stats.getTotalOccurrences(eventType);
    }

    public ListenerRegistration registerStatsUpdateListener(StatsUpdateListener listener) {
        if (listener == null) {
            return () -> {
            };
        }

        statsUpdateListeners.add(listener);
        return () -> statsUpdateListeners.remove(listener);
    }

    public ListenerRegistration registerStatsPreUpdateListener(StatsPreUpdateListener listener) {
        if (listener == null) {
            return () -> {
            };
        }

        statsPreUpdateListeners.add(listener);
        return () -> statsPreUpdateListeners.remove(listener);
    }

    private void publishStatsPreUpdate(Store<EntityStore> store, Ref<EntityStore> playerRef, NormalizedGameEvent event, PlayerStatsData playerStats) {
        if (statsPreUpdateListeners.isEmpty() || playerStats == null || event == null || event.getType() == null) {
            return;
        }

        GameEventType eventType = event.getType();
        String primaryKey = resolvePrimaryKey(eventType);
        String primaryValue = primaryKey == null ? null : event.getString(primaryKey);
        PlayerStatsData snapshotBefore = PlayerStatsData.fromPersistedStatsMap(playerStats.toPersistedStatsMap());

        StatsPreUpdate update = new StatsPreUpdate(
            store,
            playerRef,
            event,
            eventType,
            primaryKey,
            primaryValue,
            snapshotBefore.getTotalOccurrences(eventType),
            snapshotBefore.getTotalQuantity(eventType),
            snapshotBefore.getPrimaryOccurrences(eventType, primaryValue),
            snapshotBefore.getPrimaryQuantity(eventType, primaryValue),
            snapshotBefore
        );

        for (StatsPreUpdateListener listener : statsPreUpdateListeners) {
            try {
                listener.onStatsPreUpdate(update);
            } catch (Exception ex) {
                PluginLog.warn(
                    LogSource.STATS_MANAGER,
                    LogStage.HANDLE,
                    "component=StatsManager action=publishStatsPreUpdate status=listener-error message=%s",
                    ex.getMessage()
                );
            }
        }
    }

    private void publishStatsUpdated(Store<EntityStore> store, Ref<EntityStore> playerRef, NormalizedGameEvent event, PlayerStatsData playerStats) {
        if (statsUpdateListeners.isEmpty() || playerStats == null || event == null || event.getType() == null) {
            return;
        }

        GameEventType eventType = event.getType();
        String primaryKey = resolvePrimaryKey(eventType);
        String primaryValue = primaryKey == null ? null : event.getString(primaryKey);

        StatsUpdate update = new StatsUpdate(
            store,
            playerRef,
            event,
            eventType,
            primaryKey,
            primaryValue,
            playerStats.getTotalOccurrences(eventType),
            playerStats.getTotalQuantity(eventType),
            playerStats.getPrimaryOccurrences(eventType, primaryValue),
            playerStats.getPrimaryQuantity(eventType, primaryValue),
            PlayerStatsData.fromPersistedStatsMap(playerStats.toPersistedStatsMap())
        );

        for (StatsUpdateListener listener : statsUpdateListeners) {
            try {
                listener.onStatsUpdated(update);
            } catch (Exception ex) {
                PluginLog.warn(
                    LogSource.STATS_MANAGER,
                    LogStage.HANDLE,
                    "component=StatsManager action=publishStatsUpdated status=listener-error message=%s",
                    ex.getMessage()
                );
            }
        }
    }

    private static String resolvePrimaryKey(GameEventType eventType) {
        if (eventType == null) {
            return null;
        }

        return switch (eventType) {
            case BLOCK_BREAK -> "targetId";
            case BLOCK_PLACE -> "itemInHand";
            case BLOCK_USE -> "targetId";
            case CRAFT_RECIPE -> "outputId";
            case DAMAGE -> "actorId";
            case ENTITY_KILL -> "killerId";
            case INTERACTIVELY_PICKUP -> "itemId";
            case ITEM_PICKUP -> "itemId";
            case ZONE_AND_BIOME_DISCOVERY -> "zoneId";
        };
    }

    private static final class DeferredPlayerQueue {
        private final ArrayDeque<NormalizedGameEvent> events = new ArrayDeque<>();
        private boolean flushScheduled;
    }
}