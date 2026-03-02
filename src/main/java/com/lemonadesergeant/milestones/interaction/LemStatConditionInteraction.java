package com.lemonadesergeant.milestones.interaction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.lemonadesergeant.milestones.MilestonesPlugin;
import com.lemonadesergeant.milestones.data.GameEventType;
import com.lemonadesergeant.milestones.data.PlayerStatsComponent;
import com.lemonadesergeant.milestones.data.PlayerStatsData;

public class LemStatConditionInteraction extends SimpleInstantInteraction {

    public static final String TYPE_ID = "LemStatCondition";

    @Nonnull
    public static final BuilderCodec<LemStatConditionInteraction> CODEC = BuilderCodec.builder(
        LemStatConditionInteraction.class,
        LemStatConditionInteraction::new,
        SimpleInstantInteraction.CODEC
    )
        .documentation("Checks a player stat threshold and marks the interaction as Finished or Failed.")
        .<String>appendInherited(
            new KeyedCodec<>("StatType", Codec.STRING),
            (interaction, value) -> interaction.statType = value,
            interaction -> interaction.statType,
            (interaction, parent) -> interaction.statType = parent.statType
        )
        .add()
        .<String>appendInherited(
            new KeyedCodec<>("PrimaryId", Codec.STRING),
            (interaction, value) -> interaction.primaryId = value,
            interaction -> interaction.primaryId,
            (interaction, parent) -> interaction.primaryId = parent.primaryId
        )
        .add()
        .<Long>appendInherited(
            new KeyedCodec<>("MinimumOccurrences", Codec.LONG),
            (interaction, value) -> interaction.minimumOccurrences = value == null ? 1L : Math.max(0L, value),
            interaction -> interaction.minimumOccurrences,
            (interaction, parent) -> interaction.minimumOccurrences = parent.minimumOccurrences
        )
        .add()
        .<Long>appendInherited(
            new KeyedCodec<>("MinimumQuantity", Codec.LONG),
            (interaction, value) -> interaction.minimumQuantity = value == null ? 0L : Math.max(0L, value),
            interaction -> interaction.minimumQuantity,
            (interaction, parent) -> interaction.minimumQuantity = parent.minimumQuantity
        )
        .add()
        .build();

    @Nullable
    protected String statType;
    @Nullable
    protected String primaryId;
    protected long minimumOccurrences = 1L;
    protected long minimumQuantity = 0L;

    public LemStatConditionInteraction(String id) {
        super(id);
    }

    protected LemStatConditionInteraction() {
    }

    @Override
    protected void firstRun(
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        Ref<EntityStore> entityRef = context.getEntity();
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();

        if (entityRef == null || commandBuffer == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        GameEventType eventType = parseEventType(statType);
        if (eventType == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        ComponentType<EntityStore, PlayerStatsComponent> statsComponentType = MilestonesPlugin.instance().getPlayerStatsComponentType();
        PlayerStatsComponent statsComponent = commandBuffer.getComponent(entityRef, statsComponentType);
        PlayerStatsData stats = statsComponent == null ? null : statsComponent.getStatsData();

        if (stats == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        long occurrences;
        long quantity;

        if (primaryId != null && !primaryId.isBlank()) {
            occurrences = stats.getPrimaryOccurrences(eventType, primaryId);
            quantity = stats.getPrimaryQuantity(eventType, primaryId);
        } else {
            occurrences = stats.getTotalOccurrences(eventType);
            quantity = stats.getTotalQuantity(eventType);
        }

        boolean passes = occurrences >= minimumOccurrences && quantity >= minimumQuantity;
        context.getState().state = passes ? InteractionState.Finished : InteractionState.Failed;
    }

    @Nullable
    private static GameEventType parseEventType(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return GameEventType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
