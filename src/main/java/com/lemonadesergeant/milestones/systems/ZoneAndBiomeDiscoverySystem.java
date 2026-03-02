package com.lemonadesergeant.milestones.systems;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DiscoverZoneEvent;
import com.hypixel.hytale.server.core.event.events.ecs.DiscoverZoneEvent.Display;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.lemonadesergeant.milestones.data.GameEventType;
import com.lemonadesergeant.milestones.data.NormalizedGameEvent;
import com.lemonadesergeant.milestones.logging.LogStage;
import com.lemonadesergeant.milestones.logging.LogSource;
import com.lemonadesergeant.milestones.logging.PluginLog;
import com.lemonadesergeant.milestones.managers.EventForwardingManager;
public class ZoneAndBiomeDiscoverySystem  extends EntityEventSystem<EntityStore, DiscoverZoneEvent.Display> {

    public ZoneAndBiomeDiscoverySystem() {
        super(DiscoverZoneEvent.Display.class);
        PluginLog.info(LogSource.ZONE_AND_BIOME_DISCOVERY, LogStage.SETUP, "system=ZoneAndBiomeDiscoverySystem action=init eventType=%s", DiscoverZoneEvent.Display.class.getName());
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int entityIndex, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Display event) {
        var discoveryInfo = event.getDiscoveryInfo();
        String currentBiomeId = resolveCurrentBiomeId(chunk, entityIndex);
        String resolvedBiomeId = resolveBiomeId(event, discoveryInfo, currentBiomeId);

        PluginLog.info(
            LogSource.ZONE_AND_BIOME_DISCOVERY,
            LogStage.HANDLE,
            "component=ZoneAndBiomeDiscoverySystem action=handle zoneId=%s regionId=%s biomeId=%s",
            discoveryInfo == null ? "null" : discoveryInfo.zoneName(),
            discoveryInfo == null ? "null" : discoveryInfo.regionName(),
            resolvedBiomeId
        );

        SystemPlayerContextResolver.PlayerContext playerContext = SystemPlayerContextResolver.resolve(chunk, entityIndex);

        EventForwardingManager.forward(
            store,
            playerContext.playerRef,
            NormalizedGameEvent.of(GameEventType.ZONE_AND_BIOME_DISCOVERY)
                .put("playerId", playerContext.playerIdOrEntity(entityIndex))
                .put("zoneId", discoveryInfo == null ? null : discoveryInfo.zoneName())
                .put("regionId", discoveryInfo == null ? null : discoveryInfo.regionName())
                .put("biomeId", resolvedBiomeId)
                .put("currentBiomeId", currentBiomeId)
        );
    }

    private String resolveBiomeId(Display event, Object discoveryInfo, String currentBiome) {
        String biomeId = resolveBiomeIdFromObject(discoveryInfo);
        if (biomeId != null) {
            return biomeId;
        }

        Object details = invokeGetter(discoveryInfo, "details");
        if (details == null) {
            details = invokeGetter(discoveryInfo, "getDetails");
        }

        biomeId = resolveBiomeIdFromObject(details);
        if (biomeId != null) {
            return biomeId;
        }

        biomeId = resolveBiomeIdFromObject(event);
        if (biomeId != null) {
            return biomeId;
        }

        return currentBiome;
    }

    private String resolveBiomeIdFromObject(Object discoveryInfo) {
        if (discoveryInfo == null) {
            return null;
        }

        String[] directGetters = {
            "biomeId",
            "getBiomeId",
            "biomeIdentifier",
            "getBiomeIdentifier",
            "biomeName",
            "getBiomeName"
        };

        for (String getter : directGetters) {
            String value = invokeStringGetter(discoveryInfo, getter);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        Object[] biomeCandidates = {
            invokeGetter(discoveryInfo, "biome"),
            invokeGetter(discoveryInfo, "getBiome"),
            invokeGetter(discoveryInfo, "biomeType"),
            invokeGetter(discoveryInfo, "getBiomeType")
        };

        for (Object candidate : biomeCandidates) {
            String value = extractIdLikeValue(candidate);
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private String extractIdLikeValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof CharSequence) {
            String text = String.valueOf(value);
            return text.isBlank() ? null : text;
        }

        String[] getters = {
            "id",
            "getId",
            "identifier",
            "getIdentifier",
            "name",
            "getName"
        };

        for (String getter : getters) {
            String extracted = invokeStringGetter(value, getter);
            if (extracted != null && !extracted.isBlank()) {
                return extracted;
            }
        }

        String fallback = String.valueOf(value);
        if (fallback.contains("@") || fallback.isBlank()) {
            return null;
        }

        return fallback;
    }

    private Object invokeGetter(Object target, String methodName) {
        if (target == null) {
            return null;
        }

        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception ex) {
            return null;
        }
    }

    private String invokeStringGetter(Object target, String methodName) {
        if (target == null) {
            return null;
        }

        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            return value == null ? null : String.valueOf(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolveCurrentBiomeId(ArchetypeChunk<EntityStore> chunk, int entityIndex) {
        Object worldMapTracker = resolveWorldMapTracker(chunk, entityIndex);
        if (worldMapTracker == null) {
            return null;
        }

        String currentBiomeId = invokeStringGetter(worldMapTracker, "getCurrentBiomeId");
        if (currentBiomeId != null && !currentBiomeId.isBlank()) {
            return currentBiomeId;
        }

        currentBiomeId = invokeStringGetter(worldMapTracker, "currentBiomeId");
        if (currentBiomeId != null && !currentBiomeId.isBlank()) {
            return currentBiomeId;
        }

        String currentBiomeName = invokeStringGetter(worldMapTracker, "getCurrentBiomeName");
        if (currentBiomeName != null && !currentBiomeName.isBlank()) {
            return currentBiomeName;
        }

        return invokeStringGetter(worldMapTracker, "currentBiomeName");
    }

    private Object resolveWorldMapTracker(ArchetypeChunk<EntityStore> chunk, int entityIndex) {
        if (entityIndex < 0 || entityIndex >= chunk.size()) {
            return null;
        }

        Player player = chunk.getComponent(entityIndex, Player.getComponentType());
        if (player == null) {
            return null;
        }

        Object worldMapTracker = invokeGetter(player, "getWorldMapTracker");
        if (worldMapTracker == null) {
            return null;
        }

        return worldMapTracker;
    }

}
