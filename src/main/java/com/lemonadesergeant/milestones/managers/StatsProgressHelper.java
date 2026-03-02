package com.lemonadesergeant.milestones.managers;

import java.util.HashMap;
import java.util.Map;

import com.lemonadesergeant.milestones.data.DamageStatsLayout;
import com.lemonadesergeant.milestones.data.GameEventType;
import com.lemonadesergeant.milestones.data.NormalizedGameEvent;
import com.lemonadesergeant.milestones.data.PlayerStatsData;
import com.lemonadesergeant.milestones.logging.LogSource;
import com.lemonadesergeant.milestones.logging.LogStage;
import com.lemonadesergeant.milestones.logging.PluginLog;

public class StatsProgressHelper {

    public StatsProgressHelper() {
        PluginLog.info(LogSource.STATS_HELPER, LogStage.SETUP, "component=StatsProgressHelper action=init");
    }

    public void recordEvent(PlayerStatsData playerStats, NormalizedGameEvent event) {
        if (playerStats == null || event == null || event.getType() == null) {
            return;
        }

        GameEventType eventType = event.getType();
        RecordDelta delta;
        switch (eventType) {
            case BLOCK_BREAK -> delta = recordBlockBreak(playerStats, event);
            case BLOCK_PLACE -> delta = recordBlockPlace(playerStats, event);
            case BLOCK_USE -> delta = recordBlockUse(playerStats, event);
            case CRAFT_RECIPE -> delta = recordCraftRecipe(playerStats, event);
            case DAMAGE -> delta = recordDamage(playerStats, event);
            case ENTITY_KILL -> delta = recordEntityKill(playerStats, event);
            case INTERACTIVELY_PICKUP -> delta = recordInteractivePickup(playerStats, event);
            case ITEM_PICKUP -> delta = recordItemPickup(playerStats, event);
            case ZONE_AND_BIOME_DISCOVERY -> delta = recordZoneDiscovery(playerStats, event);
            default -> delta = RecordDelta.empty(eventType);
        }

        PluginLog.info(
            EventLogSourceResolver.resolve(event, LogSource.STATS_HELPER),
            LogStage.STATS_HELPER,
            "component=StatsProgressHelper action=recordEvent eventType=%s fieldCount=%s primary=%s preOccurrences=%s occurrenceDelta=%s postOccurrences=%s preQuantity=%s quantityDelta=%s postQuantity=%s zoneBiomeState=%s",
            event.getType(),
            event.getFields().size(),
            delta.primaryValue,
            delta.preOccurrences,
            delta.occurrenceDelta,
            delta.postOccurrences,
            delta.preQuantity,
            delta.quantityDelta,
            delta.postQuantity,
            delta.zoneBiomeState
        );
    }

    private RecordDelta recordBlockBreak(PlayerStatsData playerStats, NormalizedGameEvent event) {
        String primary = event.getString("targetId");
        String itemId = DamageStatsLayout.withDefault(event.getString("itemId"), DamageStatsLayout.UNARMED);
        Map<String, String> dimensions = dimensions("itemId", itemId);
        return applyWithDelta(playerStats, event.getType(), primary, dimensions, 1, null);
    }

    private RecordDelta recordBlockPlace(PlayerStatsData playerStats, NormalizedGameEvent event) {
        String primary = event.getString("itemInHand");
        return applyWithDelta(playerStats, event.getType(), primary, Map.of(), 1, null);
    }

    private RecordDelta recordBlockUse(PlayerStatsData playerStats, NormalizedGameEvent event) {
        String primary = event.getString("targetId");
        Map<String, String> dimensions = dimensions(
            "interaction", event.getString("interaction")
        );
        return applyWithDelta(playerStats, event.getType(), primary, dimensions, 1, null);
    }

    private RecordDelta recordCraftRecipe(PlayerStatsData playerStats, NormalizedGameEvent event) {
        String primary = event.getString("outputId");
        long quantityDelta = numberOrDefault(event.getLong("amount"), 1);
        Map<String, String> dimensions = dimensions(
            "recipeId", event.getString("recipeId")
        );
        return applyWithDelta(playerStats, event.getType(), primary, dimensions, quantityDelta, null);
    }

    private RecordDelta recordDamage(PlayerStatsData playerStats, NormalizedGameEvent event) {
        String primary = event.getString("actorId");
        String direction = DamageStatsLayout.normalizeDirection(event.getString("direction"));
        String counterparty = DamageStatsLayout.withDefault(event.getString("counterpartyKey"), DamageStatsLayout.UNKNOWN);
        String causeId = DamageStatsLayout.withDefault(event.getString("causeId"), DamageStatsLayout.UNKNOWN);
        String inHandId = DamageStatsLayout.withDefault(event.getString("inHandId"), DamageStatsLayout.UNARMED);

        long quantityDelta = numberOrDefault(event.getLong("amount"), 1);
        String damagePath = DamageStatsLayout.buildDamagePath(direction, counterparty, causeId, inHandId);

        Map<String, String> dimensions = dimensions(DamageStatsLayout.DIMENSION_DAMAGE_PATH, damagePath);
        return applyWithDelta(playerStats, event.getType(), primary, dimensions, quantityDelta, null);
    }

    private RecordDelta recordEntityKill(PlayerStatsData playerStats, NormalizedGameEvent event) {
        String primary = event.getString("killerId");
        Map<String, String> dimensions = dimensions(
            "victimId", event.getString("victimId"),
            "causeId", event.getString("causeId")
        );
        return applyWithDelta(playerStats, event.getType(), primary, dimensions, 1, null);
    }

    private RecordDelta recordInteractivePickup(PlayerStatsData playerStats, NormalizedGameEvent event) {
        String primary = event.getString("itemId");
        long quantityDelta = numberOrDefault(event.getLong("amount"), 1);
        Map<String, String> dimensions = dimensions(
            "blockKey", event.getString("blockKey")
        );
        return applyWithDelta(playerStats, event.getType(), primary, dimensions, quantityDelta, null);
    }

    private RecordDelta recordItemPickup(PlayerStatsData playerStats, NormalizedGameEvent event) {
        String primary = event.getString("itemId");
        if (primary != null && primary.startsWith("EditorTool_")) {
            return RecordDelta.empty(event.getType());
        }
        long quantityDelta = numberOrDefault(event.getLong("amount"), 1);
        return applyWithDelta(playerStats, event.getType(), primary, Map.of(), quantityDelta, null);
    }

    private RecordDelta recordZoneDiscovery(PlayerStatsData playerStats, NormalizedGameEvent event) {
        String primary = event.getString("zoneId");
        String preZoneBiomeState = String.valueOf(playerStats.getZoneBiomeDiscoveryMap());
        Map<String, String> dimensions = dimensions(
            "regionId", event.getString("regionId"),
            "biomeId", event.getString("biomeId")
        );
        RecordDelta delta = applyWithDelta(playerStats, event.getType(), primary, dimensions, 1, null);
        return delta.withZoneBiomeState("pre=" + preZoneBiomeState + " post=" + playerStats.getZoneBiomeDiscoveryMap());
    }

    private RecordDelta applyWithDelta(
        PlayerStatsData playerStats,
        GameEventType eventType,
        String primaryValue,
        Map<String, String> dimensions,
        long quantityDelta,
        String zoneBiomeState
    ) {
        long preOccurrences = playerStats.getPrimaryOccurrences(eventType, primaryValue);
        long preQuantity = playerStats.getPrimaryQuantity(eventType, primaryValue);

        playerStats.record(eventType, primaryValue, dimensions, quantityDelta);

        long postOccurrences = playerStats.getPrimaryOccurrences(eventType, primaryValue);
        long postQuantity = playerStats.getPrimaryQuantity(eventType, primaryValue);

        return new RecordDelta(
            primaryValue,
            preOccurrences,
            1,
            postOccurrences,
            preQuantity,
            quantityDelta,
            postQuantity,
            zoneBiomeState
        );
    }

    private long numberOrDefault(Long value, long defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * Builds a dimensions map from key/value pairs while skipping blank entries.
     */
    private Map<String, String> dimensions(String... keyValues) {
        Map<String, String> dimensions = new HashMap<>();
        if (keyValues == null || keyValues.length == 0) {
            return dimensions;
        }

        int pairCount = keyValues.length - (keyValues.length % 2);
        for (int index = 0; index < pairCount; index += 2) {
            putIfPresent(dimensions, keyValues[index], keyValues[index + 1]);
        }

        return dimensions;
    }

    private void putIfPresent(Map<String, String> dimensions, String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            return;
        }

        dimensions.put(key, value);
    }

    private static final class RecordDelta {
        private final String primaryValue;
        private final long preOccurrences;
        private final long occurrenceDelta;
        private final long postOccurrences;
        private final long preQuantity;
        private final long quantityDelta;
        private final long postQuantity;
        private final String zoneBiomeState;

        private RecordDelta(
            String primaryValue,
            long preOccurrences,
            long occurrenceDelta,
            long postOccurrences,
            long preQuantity,
            long quantityDelta,
            long postQuantity,
            String zoneBiomeState
        ) {
            this.primaryValue = primaryValue;
            this.preOccurrences = preOccurrences;
            this.occurrenceDelta = occurrenceDelta;
            this.postOccurrences = postOccurrences;
            this.preQuantity = preQuantity;
            this.quantityDelta = quantityDelta;
            this.postQuantity = postQuantity;
            this.zoneBiomeState = zoneBiomeState;
        }

        private RecordDelta withZoneBiomeState(String value) {
            return new RecordDelta(
                primaryValue,
                preOccurrences,
                occurrenceDelta,
                postOccurrences,
                preQuantity,
                quantityDelta,
                postQuantity,
                value
            );
        }

        private static RecordDelta empty(GameEventType eventType) {
            return new RecordDelta(eventType == null ? null : eventType.name(), 0, 0, 0, 0, 0, 0, null);
        }
    }
}