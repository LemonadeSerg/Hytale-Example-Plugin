package com.lemonadesergeant.milestones.api;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.lemonadesergeant.milestones.data.GameEventType;
import com.lemonadesergeant.milestones.data.PlayerStatsData;
import com.lemonadesergeant.milestones.managers.EventForwardingManager;
import com.lemonadesergeant.milestones.managers.StatsManager;

public final class MilestonesStatsApi {

    private MilestonesStatsApi() {
    }

    @Nonnull
    public static PlayerStatsData getPlayerStats(@Nullable Store<EntityStore> store, @Nullable Ref<EntityStore> playerRef) {
        return statsManager().getOrCreatePlayerStats(store, playerRef);
    }

    public static long getTotalOccurrences(
        @Nullable Store<EntityStore> store,
        @Nullable Ref<EntityStore> playerRef,
        @Nullable GameEventType eventType
    ) {
        if (eventType == null) {
            return 0;
        }

        return statsManager().getTotalEventOccurrences(store, playerRef, eventType);
    }

    public static long getTotalQuantity(
        @Nullable Store<EntityStore> store,
        @Nullable Ref<EntityStore> playerRef,
        @Nullable GameEventType eventType
    ) {
        if (eventType == null) {
            return 0;
        }

        return getPlayerStats(store, playerRef).getTotalQuantity(eventType);
    }

    @Nonnull
    public static Map<String, Long> getPrimaryOccurrences(
        @Nullable Store<EntityStore> store,
        @Nullable Ref<EntityStore> playerRef,
        @Nullable GameEventType eventType
    ) {
        if (eventType == null) {
            return Map.of();
        }

        return getPlayerStats(store, playerRef).getPrimaryOccurrenceBreakdown(eventType);
    }

    @Nonnull
    public static ListenerRegistration onStatsUpdated(@Nullable StatsUpdateListener listener) {
        if (listener == null) {
            return () -> {
            };
        }

        return statsManager().registerStatsUpdateListener(listener);
    }

    @Nonnull
    public static ListenerRegistration onStatsPreUpdate(@Nullable StatsPreUpdateListener listener) {
        if (listener == null) {
            return () -> {
            };
        }

        return statsManager().registerStatsPreUpdateListener(listener);
    }

    @Nonnull
    private static StatsManager statsManager() {
        return EventForwardingManager.getStatsManager();
    }
}