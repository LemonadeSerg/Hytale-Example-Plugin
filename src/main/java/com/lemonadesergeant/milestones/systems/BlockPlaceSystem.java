package com.lemonadesergeant.milestones.systems;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.lemonadesergeant.milestones.data.GameEventType;
import com.lemonadesergeant.milestones.data.NormalizedGameEvent;
import com.lemonadesergeant.milestones.logging.LogStage;
import com.lemonadesergeant.milestones.logging.LogSource;
import com.lemonadesergeant.milestones.logging.PluginLog;
import com.lemonadesergeant.milestones.managers.EventForwardingManager;

public class BlockPlaceSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
    
    public BlockPlaceSystem() {
        super(PlaceBlockEvent.class);
        PluginLog.info(LogSource.BLOCK_PLACE, LogStage.SETUP, "system=BlockPlaceSystem action=init eventType=%s", PlaceBlockEvent.class.getName());
    }
    
    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
        public void handle(int entityIndex, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull PlaceBlockEvent event) {
        SystemPlayerContextResolver.PlayerContext playerContext = SystemPlayerContextResolver.resolve(chunk, entityIndex);

        PluginLog.info(
            LogSource.BLOCK_PLACE,
            LogStage.HANDLE,
            "component=BlockPlaceSystem action=handle playerId=%s targetId=%s itemInHand=%s cancelled=%s",
            playerContext.playerIdOrEntity(entityIndex),
            resolveBlockTypeId(event),
            SystemEventValueResolver.resolveItemId(event.getItemInHand()),
            event.isCancelled()
        );

        EventForwardingManager.forward(
            store,
            playerContext.playerRef,
            NormalizedGameEvent.of(GameEventType.BLOCK_PLACE)
                .put("targetId", resolveBlockTypeId(event))
                .put("playerId", playerContext.playerIdOrEntity(entityIndex))
                .put("itemInHand", SystemEventValueResolver.resolveItemId(event.getItemInHand()))
                .put("cancelled", event.isCancelled())
        );
    }

    private String resolveBlockTypeId(PlaceBlockEvent event) {
        if (event == null) {
            return null;
        }

        try {
            Object blockType = event.getClass().getMethod("getBlockType").invoke(event);
            if (blockType != null) {
                Object id = blockType.getClass().getMethod("getId").invoke(blockType);
                return id == null ? null : String.valueOf(id);
            }
        } catch (Exception ignored) {
        }

        try {
            Object blockState = event.getClass().getMethod("getBlockState").invoke(event);
            if (blockState == null) {
                return null;
            }

            Object blockType = blockState.getClass().getMethod("getBlockType").invoke(blockState);
            if (blockType == null) {
                return null;
            }

            Object id = blockType.getClass().getMethod("getId").invoke(blockType);
            return id == null ? null : String.valueOf(id);
        } catch (Exception ex) {
            return null;
        }
    }

}
