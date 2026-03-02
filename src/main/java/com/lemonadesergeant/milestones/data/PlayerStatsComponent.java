package com.lemonadesergeant.milestones.data;

import java.util.Map;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PlayerStatsComponent implements Component<EntityStore> {

    public static final BuilderCodec<PlayerStatsComponent> CODEC = BuilderCodec.builder(PlayerStatsComponent.class, PlayerStatsComponent::new)
        .append(
            new KeyedCodec<>("Stats", PlayerStatsData.PERSISTED_STATS_CODEC),
            PlayerStatsComponent::setPersistedStats,
            PlayerStatsComponent::getPersistedStats
        )
        .add()
        .build();

    private PlayerStatsData statsData;

    public PlayerStatsComponent() {
        this.statsData = new PlayerStatsData();
    }

    public PlayerStatsComponent(PlayerStatsComponent copyFrom) {
        if (copyFrom == null) {
            this.statsData = new PlayerStatsData();
            return;
        }

        this.statsData = PlayerStatsData.fromPersistedStatsMap(copyFrom.getStatsData().toPersistedStatsMap());
    }

    public PlayerStatsData getStatsData() {
        if (statsData == null) {
            statsData = new PlayerStatsData();
        }
        return statsData;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new PlayerStatsComponent(this);
    }

    private Map<String, Map<String, PlayerStatsData.PersistedPrimaryStats>> getPersistedStats() {
        return getStatsData().toPersistedStatsMap();
    }

    private void setPersistedStats(Map<String, Map<String, PlayerStatsData.PersistedPrimaryStats>> stats) {
        statsData = PlayerStatsData.fromPersistedStatsMap(stats);
    }
}