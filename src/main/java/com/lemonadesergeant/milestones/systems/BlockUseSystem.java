package com.lemonadesergeant.milestones.systems;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent.Post;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.lemonadesergeant.milestones.data.GameEventType;
import com.lemonadesergeant.milestones.data.NormalizedGameEvent;
import com.lemonadesergeant.milestones.logging.LogStage;
import com.lemonadesergeant.milestones.logging.LogSource;
import com.lemonadesergeant.milestones.logging.PluginLog;
import com.lemonadesergeant.milestones.managers.EventForwardingManager;
public class BlockUseSystem  extends EntityEventSystem<EntityStore, UseBlockEvent.Post> {

    public BlockUseSystem() {
        super(UseBlockEvent.Post.class);
        PluginLog.info(LogSource.BLOCK_USE, LogStage.SETUP, "system=BlockUseSystem action=init eventType=%s", UseBlockEvent.Post.class.getName());
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
        public void handle(int entityIndex, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Post event) {
        SystemPlayerContextResolver.PlayerContext playerContext = SystemPlayerContextResolver.resolve(chunk, entityIndex);

        PluginLog.info(
            LogSource.BLOCK_USE,
            LogStage.HANDLE,
            "component=BlockUseSystem action=handle playerId=%s targetId=%s itemId=%s interaction=%s",
            playerContext.playerIdOrEntity(entityIndex),
            event.getBlockType() == null ? null : event.getBlockType().getId(),
            SystemEventValueResolver.resolveItemId(event.getContext().getHeldItem()),
            event.getInteractionType()
        );

        EventForwardingManager.forward(
            store,
            playerContext.playerRef,
            NormalizedGameEvent.of(GameEventType.BLOCK_USE)
                .put("targetId", event.getBlockType() == null ? null : event.getBlockType().getId())
                .put("playerId", playerContext.playerIdOrEntity(entityIndex))
                .put("itemId", SystemEventValueResolver.resolveItemId(event.getContext().getHeldItem()))
                .put("interaction", event.getInteractionType())
        );
    }
}
