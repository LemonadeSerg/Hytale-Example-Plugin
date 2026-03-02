package com.lemonadesergeant.milestones.systems;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.lemonadesergeant.milestones.data.GameEventType;
import com.lemonadesergeant.milestones.data.NormalizedGameEvent;
import com.lemonadesergeant.milestones.logging.LogSource;
import com.lemonadesergeant.milestones.logging.LogStage;
import com.lemonadesergeant.milestones.logging.PluginLog;
import com.lemonadesergeant.milestones.managers.EventForwardingManager;

public class EntityKillSystem extends EntityEventSystem<EntityStore, KillFeedEvent.KillerMessage> {

    public EntityKillSystem() {
        super(KillFeedEvent.KillerMessage.class);
        PluginLog.info(LogSource.ENTITY_KILL, LogStage.SETUP, "system=EntityKillSystem action=init eventType=%s", KillFeedEvent.KillerMessage.class.getName());
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int entityIndex, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull KillFeedEvent.KillerMessage event) {
        SystemPlayerContextResolver.PlayerContext killerContext = SystemPlayerContextResolver.resolve(chunk, entityIndex);
        Ref<EntityStore> killerRef = killerContext.playerRef;
        Player killerPlayer = killerContext.player;

        Ref<EntityStore> targetRef = event.getTargetRef();
        Player targetPlayer = SystemEntityResolver.resolvePlayer(targetRef);
        Ref<EntityStore> statsPlayerRef = killerPlayer != null ? killerRef : targetRef;

        if (killerPlayer == null && targetPlayer == null) {
            return;
        }

        Damage damage = event.getDamage();
        PluginLog.info(
            LogSource.ENTITY_KILL,
            LogStage.HANDLE,
            "component=EntityKillSystem action=handle killerId=%s killerName=%s victimId=%s victimName=%s causeId=%s amount=%s",
            SystemEntityResolver.resolveEntityId(killerRef, killerPlayer),
            SystemEntityResolver.resolveEntityName(killerRef, killerPlayer),
            SystemEntityResolver.resolveEntityId(targetRef, targetPlayer),
            SystemEntityResolver.resolveEntityName(targetRef, targetPlayer),
            damage.getCause() == null ? null : damage.getCause().getId(),
            damage.getAmount()
        );

        EventForwardingManager.forward(
            store,
            statsPlayerRef,
            NormalizedGameEvent.of(GameEventType.ENTITY_KILL)
                .put("killerId", SystemEntityResolver.resolveEntityId(killerRef, killerPlayer))
                .put("killerName", SystemEntityResolver.resolveEntityName(killerRef, killerPlayer))
                .put("victimId", SystemEntityResolver.resolveEntityId(targetRef, targetPlayer))
                .put("victimName", SystemEntityResolver.resolveEntityName(targetRef, targetPlayer))
                .put("causeId", damage.getCause() == null ? null : damage.getCause().getId())
                .put("amount", damage.getAmount())
        );
    }
}
