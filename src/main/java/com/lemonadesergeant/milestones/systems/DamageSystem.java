package com.lemonadesergeant.milestones.systems;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.lemonadesergeant.milestones.data.DamageStatsLayout;
import com.lemonadesergeant.milestones.data.GameEventType;
import com.lemonadesergeant.milestones.data.NormalizedGameEvent;
import com.lemonadesergeant.milestones.logging.LogStage;
import com.lemonadesergeant.milestones.logging.LogSource;
import com.lemonadesergeant.milestones.logging.PluginLog;
import com.lemonadesergeant.milestones.managers.EventForwardingManager;
public class DamageSystem  extends EntityEventSystem<EntityStore, Damage> {

    public DamageSystem() {
        super(Damage.class);
        PluginLog.info(LogSource.DAMAGE, LogStage.SETUP, "system=DamageSystem action=init eventType=%s", Damage.class.getName());
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
        public void handle(int entityIndex, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage event) {
        SystemPlayerContextResolver.PlayerContext targetContext = SystemPlayerContextResolver.resolve(chunk, entityIndex);
        Ref<EntityStore> targetRef = targetContext.playerRef;
        Player targetPlayer = targetContext.player;

        Ref<EntityStore> sourceRef = resolveSourceRef(event);
        Player sourcePlayer = SystemEntityResolver.resolvePlayer(sourceRef);

        if (sourcePlayer == null && targetPlayer == null) {
            return;
        }

        String sourceEntityId = SystemEntityResolver.resolveEntityId(sourceRef, sourcePlayer);
        String sourceEntityName = SystemEntityResolver.resolveEntityName(sourceRef, sourcePlayer);
        String targetEntityId = SystemEntityResolver.resolveEntityId(targetRef, targetPlayer);
        String targetEntityName = SystemEntityResolver.resolveEntityName(targetRef, targetPlayer);
        String sourceStatKey = SystemEntityResolver.resolveEntityStatKey(sourceRef, sourcePlayer);
        String targetStatKey = SystemEntityResolver.resolveEntityStatKey(targetRef, targetPlayer);
        String sourceInHandId = resolveSourceInHandId(sourcePlayer, event);

        PluginLog.info(
            LogSource.DAMAGE,
            LogStage.HANDLE,
            "component=DamageSystem action=handle sourceId=%s sourceName=%s targetId=%s targetName=%s amount=%s",
            sourceEntityId,
            sourceEntityName,
            targetEntityId,
            targetEntityName,
            event.getAmount()
        );

        if (sourcePlayer != null) {
            EventForwardingManager.forward(
                store,
                sourceRef,
                NormalizedGameEvent.of(GameEventType.DAMAGE)
                    .put("actorId", sourceEntityId)
                    .put("direction", DamageStatsLayout.DIRECTION_DEALT)
                    .put("counterpartyKey", targetStatKey)
                    .put("counterpartyId", targetEntityId)
                    .put("counterpartyName", targetEntityName)
                    .put("inHandId", sourceInHandId)
                    .put("amount", event.getAmount())
                    .put("causeId", event.getCause() == null ? null : event.getCause().getId())
            );
        }

        if (targetPlayer != null) {
            EventForwardingManager.forward(
                store,
                targetRef,
                NormalizedGameEvent.of(GameEventType.DAMAGE)
                    .put("actorId", targetEntityId)
                    .put("direction", DamageStatsLayout.DIRECTION_TAKEN)
                    .put("counterpartyKey", sourceStatKey)
                    .put("counterpartyId", sourceEntityId)
                    .put("counterpartyName", sourceEntityName)
                    .put("inHandId", sourceInHandId)
                    .put("amount", event.getAmount())
                    .put("causeId", event.getCause() == null ? null : event.getCause().getId())
            );
        }
    }

    private Ref<EntityStore> resolveSourceRef(@Nonnull Damage damage) {
        Damage.Source source = damage.getSource();
        if (source instanceof Damage.EntitySource) {
            return ((Damage.EntitySource) source).getRef();
        }
        return null;
    }

    private String resolveSourceInHandId(Player sourcePlayer, Damage event) {
        if (sourcePlayer == null) {
            return DamageStatsLayout.UNARMED;
        }

        try {
            Object item = sourcePlayer.getClass().getMethod("getItemInHand").invoke(sourcePlayer);
            String itemId = SystemEventValueResolver.resolveItemId(item);
            if (itemId != null && !itemId.isBlank()) {
                return itemId;
            }
        } catch (Exception ex) {
        }

        return DamageStatsLayout.UNARMED;
    }

}