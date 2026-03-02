package com.lemonadesergeant.milestones.systems;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent;
import com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent.Post;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.lemonadesergeant.milestones.data.GameEventType;
import com.lemonadesergeant.milestones.data.NormalizedGameEvent;
import com.lemonadesergeant.milestones.logging.LogStage;
import com.lemonadesergeant.milestones.logging.LogSource;
import com.lemonadesergeant.milestones.logging.PluginLog;
import com.lemonadesergeant.milestones.managers.EventForwardingManager;
public class CraftRecipeSystem  extends EntityEventSystem<EntityStore, CraftRecipeEvent.Post> {

    public CraftRecipeSystem() {
        super(CraftRecipeEvent.Post.class);
        PluginLog.info(LogSource.CRAFT_RECIPE, LogStage.SETUP, "system=CraftRecipeSystem action=init eventType=%s", CraftRecipeEvent.Post.class.getName());
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
            LogSource.CRAFT_RECIPE,
            LogStage.HANDLE,
            "component=CraftRecipeSystem action=handle playerId=%s recipeId=%s outputId=%s amount=%s",
            playerContext.playerIdOrEntity(entityIndex),
            resolveRecipeId(event.getCraftedRecipe()),
            SystemEventValueResolver.resolveItemId(event.getCraftedRecipe() == null ? null : event.getCraftedRecipe().getPrimaryOutput()),
            event.getQuantity()
        );

        EventForwardingManager.forward(
            store,
            playerContext.playerRef,
            NormalizedGameEvent.of(GameEventType.CRAFT_RECIPE)
                .put("recipeId", resolveRecipeId(event.getCraftedRecipe()))
                .put("playerId", playerContext.playerIdOrEntity(entityIndex))
                .put("outputId", SystemEventValueResolver.resolveItemId(event.getCraftedRecipe() == null ? null : event.getCraftedRecipe().getPrimaryOutput()))
                .put("amount", event.getQuantity())
        );
    }

    private String resolveRecipeId(Object craftedRecipe) {
        if (craftedRecipe == null) {
            return null;
        }

        try {
            Object value = craftedRecipe.getClass().getMethod("getId").invoke(craftedRecipe);
            return value == null ? null : String.valueOf(value);
        } catch (Exception ex) {
            return null;
        }
    }

}
