package com.lemonadesergeant.milestones.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.lemonadesergeant.milestones.data.GameEventType;
import com.lemonadesergeant.milestones.data.NormalizedGameEvent;
import com.lemonadesergeant.milestones.data.PlayerStatsData;

public final class StatsUpdate {

    private final Store<EntityStore> store;
    private final Ref<EntityStore> playerRef;
    private final NormalizedGameEvent event;
    private final GameEventType eventType;
    private final Map<String, Object> eventFields;
    private final String primaryKey;
    private final String primaryValue;
    private final long totalOccurrencesAfter;
    private final long totalQuantityAfter;
    private final long primaryOccurrencesAfter;
    private final long primaryQuantityAfter;
    private final PlayerStatsData statsSnapshot;

    public StatsUpdate(
        @Nullable Store<EntityStore> store,
        @Nullable Ref<EntityStore> playerRef,
        @Nullable NormalizedGameEvent event,
        @Nullable GameEventType eventType,
        @Nullable String primaryKey,
        @Nullable String primaryValue,
        long totalOccurrencesAfter,
        long totalQuantityAfter,
        long primaryOccurrencesAfter,
        long primaryQuantityAfter,
        @Nonnull PlayerStatsData statsSnapshot
    ) {
        this.store = store;
        this.playerRef = playerRef;
        this.event = event;
        this.eventType = eventType;
        this.primaryKey = primaryKey;
        this.primaryValue = primaryValue;
        this.totalOccurrencesAfter = totalOccurrencesAfter;
        this.totalQuantityAfter = totalQuantityAfter;
        this.primaryOccurrencesAfter = primaryOccurrencesAfter;
        this.primaryQuantityAfter = primaryQuantityAfter;
        this.statsSnapshot = statsSnapshot;

        Map<String, Object> fields = event == null ? Map.of() : new LinkedHashMap<>(event.getFields());
        this.eventFields = Collections.unmodifiableMap(fields);
    }

    @Nullable
    public Store<EntityStore> getStore() {
        return store;
    }

    @Nullable
    public Ref<EntityStore> getPlayerRef() {
        return playerRef;
    }

    @Nullable
    public NormalizedGameEvent getEvent() {
        return event;
    }

    @Nullable
    public GameEventType getEventType() {
        return eventType;
    }

    @Nonnull
    public Map<String, Object> getEventFields() {
        return eventFields;
    }

    @Nullable
    public String getPrimaryKey() {
        return primaryKey;
    }

    @Nullable
    public String getPrimaryValue() {
        return primaryValue;
    }

    public long getTotalOccurrencesAfter() {
        return totalOccurrencesAfter;
    }

    public long getTotalQuantityAfter() {
        return totalQuantityAfter;
    }

    public long getPrimaryOccurrencesAfter() {
        return primaryOccurrencesAfter;
    }

    public long getPrimaryQuantityAfter() {
        return primaryQuantityAfter;
    }

    @Nonnull
    public PlayerStatsData getStatsSnapshot() {
        return statsSnapshot;
    }

    @Nullable
    public String getString(@Nonnull String key) {
        Object value = eventFields.get(key);
        if (value == null) {
            return null;
        }

        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    @Nullable
    public Long getLong(@Nonnull String key) {
        Object value = eventFields.get(key);
        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }
}