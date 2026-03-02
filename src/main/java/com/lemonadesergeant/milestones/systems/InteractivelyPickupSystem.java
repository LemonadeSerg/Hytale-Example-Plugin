package com.lemonadesergeant.milestones.systems;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.lemonadesergeant.milestones.data.GameEventType;
import com.lemonadesergeant.milestones.data.NormalizedGameEvent;
import com.lemonadesergeant.milestones.logging.LogStage;
import com.lemonadesergeant.milestones.logging.LogSource;
import com.lemonadesergeant.milestones.logging.PluginLog;
import com.lemonadesergeant.milestones.managers.EventForwardingManager;
public class InteractivelyPickupSystem  extends EntityEventSystem<EntityStore, InteractivelyPickupItemEvent> {

    public InteractivelyPickupSystem() {
        super(InteractivelyPickupItemEvent.class);
        PluginLog.info(LogSource.INTERACTIVELY_PICKUP, LogStage.SETUP, "system=InteractivelyPickupSystem action=init eventType=%s", InteractivelyPickupItemEvent.class.getName());
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
        public void handle(int entityIndex, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractivelyPickupItemEvent event) {
        SystemPlayerContextResolver.PlayerContext playerContext = SystemPlayerContextResolver.resolve(chunk, entityIndex);

        PluginLog.info(
            LogSource.INTERACTIVELY_PICKUP,
            LogStage.HANDLE,
            "component=InteractivelyPickupSystem action=handle playerId=%s itemId=%s amount=%s blockKey=%s",
            playerContext.playerIdOrEntity(entityIndex),
            event.getItemStack() == null ? null : event.getItemStack().getItemId(),
            event.getItemStack() == null ? 0 : event.getItemStack().getQuantity(),
            event.getItemStack() == null ? null : event.getItemStack().getBlockKey()
        );

        EventForwardingManager.forward(
            store,
            playerContext.playerRef,
            NormalizedGameEvent.of(GameEventType.INTERACTIVELY_PICKUP)
                .put("playerId", playerContext.playerIdOrEntity(entityIndex))
                .put("itemId", event.getItemStack() == null ? null : event.getItemStack().getItemId())
                .put("amount", event.getItemStack() == null ? 0 : event.getItemStack().getQuantity())
                .put("blockKey", event.getItemStack() == null ? null : event.getItemStack().getBlockKey())
        );
    }
}
