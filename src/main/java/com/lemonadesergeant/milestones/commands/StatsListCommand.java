package com.lemonadesergeant.milestones.commands;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.lemonadesergeant.milestones.data.GameEventType;
import com.lemonadesergeant.milestones.data.PlayerStatsData;
import com.lemonadesergeant.milestones.managers.EventForwardingManager;
import com.lemonadesergeant.milestones.managers.StatsManager;

public class StatsListCommand extends AbstractTargetPlayerCommand {

    @Nonnull
    private final OptionalArg<GameEventType> statTypeArg = this.withOptionalArg(
        "statType",
        "milestones.commands.stats.arg.statType.desc",
        ArgTypes.forEnum("milestones.commands.stats.arg.statType.name", GameEventType.class)
    );

    @Nonnull
    private final OptionalArg<String> idArg = this.withOptionalArg("id", "milestones.commands.stats.arg.id.desc", ArgTypes.STRING)
        .addValidator(Validators.nonEmptyString());

    public StatsListCommand() {
        super("stats", "milestones.commands.stats.desc");
        this.addSubCommand(new StatsTypesCommand());
    }

    @Override
    protected void execute(
        @Nonnull CommandContext context,
        @Nullable Ref<EntityStore> sourceRef,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world,
        @Nonnull Store<EntityStore> store
    ) {
        boolean hasStatType = statTypeArg.provided(context);
        boolean hasId = idArg.provided(context);

        if (hasId && !hasStatType) {
            context.sendMessage(Message.raw("You must provide --statType when using --id."));
            return;
        }

        StatsManager statsManager = EventForwardingManager.getStatsManager();
        PlayerStatsData stats = statsManager.getOrCreatePlayerStats(store, ref);
        String username = playerRef.getUsername();

        if (!hasStatType) {
            sendAllEventTotals(context, stats, username);
            return;
        }

        GameEventType eventType = statTypeArg.get(context);

        if (!hasId) {
            sendTypeBreakdown(context, stats, username, eventType);
            return;
        }

        sendIdDetails(context, stats, username, eventType, idArg.get(context));
    }

    private void sendAllEventTotals(@Nonnull CommandContext context, @Nonnull PlayerStatsData stats, @Nonnull String username) {
        context.sendMessage(Message.raw("Milestones stats for " + username + ":"));
        for (GameEventType eventType : GameEventType.values()) {
            long occurrences = stats.getTotalOccurrences(eventType);
            long quantity = stats.getTotalQuantity(eventType);
            context.sendMessage(Message.raw(" - " + eventType.name().toLowerCase(Locale.ROOT) + " occurrences=" + occurrences + " quantity=" + quantity));
        }
    }

    private void sendTypeBreakdown(@Nonnull CommandContext context, @Nonnull PlayerStatsData stats, @Nonnull String username, @Nonnull GameEventType eventType) {
        if (eventType == GameEventType.ZONE_AND_BIOME_DISCOVERY) {
            sendZoneBiomeOverview(context, stats, username);
            return;
        }

        Map<String, Long> primaryBreakdown = stats.getPrimaryOccurrenceBreakdown(eventType);
        context.sendMessage(Message.raw("Milestones stats for " + username + " type=" + eventType.name().toLowerCase(Locale.ROOT) + ":"));

        if (primaryBreakdown.isEmpty()) {
            context.sendMessage(Message.raw(" - no stats recorded"));
            return;
        }

        primaryBreakdown.forEach((id, occurrences) -> {
            long quantity = stats.getPrimaryQuantity(eventType, id);
            context.sendMessage(Message.raw(" - id=" + id + " occurrences=" + occurrences + " quantity=" + quantity));
        });
    }

    private void sendIdDetails(@Nonnull CommandContext context, @Nonnull PlayerStatsData stats, @Nonnull String username, @Nonnull GameEventType eventType, @Nonnull String id) {
        long occurrences = stats.getPrimaryOccurrences(eventType, id);
        long quantity = stats.getPrimaryQuantity(eventType, id);

        context.sendMessage(
            Message.raw(
                "Milestones stats for "
                    + username
                    + " type="
                    + eventType.name().toLowerCase(Locale.ROOT)
                    + " id="
                    + id
                    + " occurrences="
                    + occurrences
                    + " quantity="
                    + quantity
            )
        );

        if (eventType == GameEventType.ZONE_AND_BIOME_DISCOVERY) {
            Map<String, Set<String>> zoneMap = stats.getZoneBiomeDiscoveryMap();
            Set<String> discoveredBiomes = zoneMap.getOrDefault(id, Set.of());
            String biomeText = discoveredBiomes.isEmpty() ? "{}" : "{" + String.join(", ", discoveredBiomes) + "}";
            context.sendMessage(Message.raw(" - discoveredBiomes=" + biomeText));
            return;
        }

        List<String> dimensions = dimensionsFor(eventType);
        boolean anyDimensionData = false;
        for (String dimension : dimensions) {
            Map<String, Long> breakdown = stats.getSplitOccurrenceBreakdown(eventType, id, dimension);
            if (!breakdown.isEmpty()) {
                anyDimensionData = true;
                String values = breakdown.entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining(", "));
                context.sendMessage(Message.raw(" - " + dimension + " { " + values + " }"));
            }
        }

        if (!anyDimensionData) {
            context.sendMessage(Message.raw(" - no dimension breakdown recorded"));
        }
    }

    private void sendZoneBiomeOverview(@Nonnull CommandContext context, @Nonnull PlayerStatsData stats, @Nonnull String username) {
        Map<String, Set<String>> zoneBiomes = stats.getZoneBiomeDiscoveryMap();
        context.sendMessage(Message.raw("Milestones stats for " + username + " type=" + GameEventType.ZONE_AND_BIOME_DISCOVERY.name().toLowerCase(Locale.ROOT) + ":"));

        if (zoneBiomes.isEmpty()) {
            context.sendMessage(Message.raw(" - no stats recorded"));
            return;
        }

        zoneBiomes.forEach((zone, biomes) -> {
            String biomeText = biomes.isEmpty() ? "{}" : "{" + String.join(", ", biomes) + "}";
            context.sendMessage(Message.raw(" - " + zone + ":" + biomeText));
        });
    }

    @Nonnull
    private static List<String> dimensionsFor(@Nonnull GameEventType eventType) {
        return switch (eventType) {
            case BLOCK_BREAK -> List.of("itemId");
            case BLOCK_PLACE -> List.of("itemInHand", "cancelled");
            case BLOCK_USE -> List.of("itemId", "interaction");
            case CRAFT_RECIPE -> List.of("recipeId");
            case DAMAGE -> List.of("sourceId", "causeId");
            case ENTITY_KILL -> List.of("victimId", "causeId");
            case INTERACTIVELY_PICKUP -> List.of("blockKey");
            case ITEM_PICKUP -> List.of();
            case ZONE_AND_BIOME_DISCOVERY -> List.of("regionId", "biomeId");
        };
    }
}
