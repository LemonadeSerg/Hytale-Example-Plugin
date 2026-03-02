package com.lemonadesergeant.milestones.managers;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.lemonadesergeant.milestones.data.NormalizedGameEvent;

public final class EventForwardingManager {

    private static final StatsManager STATS_MANAGER = new StatsManager();

    private EventForwardingManager() {
    }

    public static void forward(Store<EntityStore> store, Ref<EntityStore> playerRef, NormalizedGameEvent event) {
        if (event == null) {
            return;
        }

        STATS_MANAGER.recordEvent(store, playerRef, event);
    }

    public static StatsManager getStatsManager() {
        return STATS_MANAGER;
    }
}