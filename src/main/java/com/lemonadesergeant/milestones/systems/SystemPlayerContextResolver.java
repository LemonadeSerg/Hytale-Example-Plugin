package com.lemonadesergeant.milestones.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

final class SystemPlayerContextResolver {

    private SystemPlayerContextResolver() {
    }

    static PlayerContext resolve(ArchetypeChunk<EntityStore> chunk, int entityIndex) {
        if (chunk == null || entityIndex < 0 || entityIndex >= chunk.size()) {
            return new PlayerContext(null, null);
        }

        Player player = chunk.getComponent(entityIndex, Player.getComponentType());
        Ref<EntityStore> playerRef = chunk.getReferenceTo(entityIndex);
        return new PlayerContext(player, playerRef);
    }

    static final class PlayerContext {
        final Player player;
        final Ref<EntityStore> playerRef;

        private PlayerContext(Player player, Ref<EntityStore> playerRef) {
            this.player = player;
            this.playerRef = playerRef;
        }

        String playerIdOrEntity(int entityIndex) {
            return SystemEntityResolver.resolvePlayerOrEntityId(player, entityIndex);
        }
    }
}