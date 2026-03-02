package com.lemonadesergeant.milestones.data;

final class EventStatsKeyMapper {

    private static final String UNKNOWN = "unknown";

    private EventStatsKeyMapper() {
    }

    static String toStorageEventKey(String eventKey) {
        if (eventKey == null || eventKey.isBlank()) {
            return UNKNOWN;
        }

        GameEventType eventType = parseGameEventType(eventKey);
        if (eventType == null) {
            return eventKey;
        }

        return switch (eventType) {
            case BLOCK_BREAK -> "BlockBreak";
            case BLOCK_PLACE -> "BlockPlace";
            case BLOCK_USE -> "BlockUse";
            case CRAFT_RECIPE -> "CraftRecipe";
            case DAMAGE -> "Damage";
            case ENTITY_KILL -> "EntityKill";
            case INTERACTIVELY_PICKUP -> "InteractivelyPickup";
            case ITEM_PICKUP -> "ItemPickup";
            case ZONE_AND_BIOME_DISCOVERY -> "ZoneAndBiomeDiscovery";
        };
    }

    static String toCanonicalEventKey(String eventKey) {
        if (eventKey == null || eventKey.isBlank()) {
            return UNKNOWN;
        }

        GameEventType eventType = parseGameEventType(eventKey);
        return eventType == null ? eventKey : eventType.name();
    }

    static GameEventType parseGameEventType(String eventKey) {
        if (eventKey == null || eventKey.isBlank()) {
            return null;
        }

        try {
            return GameEventType.valueOf(eventKey);
        } catch (IllegalArgumentException ex) {
        }

        String compact = eventKey
            .replace("_", "")
            .replace("-", "")
            .replace(" ", "")
            .toLowerCase();

        return switch (compact) {
            case "blockbreak" -> GameEventType.BLOCK_BREAK;
            case "blockplace" -> GameEventType.BLOCK_PLACE;
            case "blockuse" -> GameEventType.BLOCK_USE;
            case "craftrecipe" -> GameEventType.CRAFT_RECIPE;
            case "damage", "receiveddamage", "recieveddamage", "dealtdamage" -> GameEventType.DAMAGE;
            case "entitykill", "killedby" -> GameEventType.ENTITY_KILL;
            case "interactivelypickup" -> GameEventType.INTERACTIVELY_PICKUP;
            case "itempickup" -> GameEventType.ITEM_PICKUP;
            case "zoneandbiomediscovery" -> GameEventType.ZONE_AND_BIOME_DISCOVERY;
            default -> null;
        };
    }

    static String toStorageDimensionKey(String eventKey, String dimensionKey) {
        if (dimensionKey == null || dimensionKey.isBlank()) {
            return UNKNOWN;
        }

        GameEventType eventType = parseGameEventType(eventKey);
        if (eventType == GameEventType.BLOCK_BREAK && "itemId".equals(dimensionKey)) {
            return "InHandBreakDown";
        }

        if (eventType == GameEventType.DAMAGE && "causeId".equals(dimensionKey)) {
            return "DamageTypeBreakdown";
        }

        if (eventType == GameEventType.DAMAGE && "sourceId".equals(dimensionKey)) {
            return "SourceBreakdown";
        }

        if (eventType == GameEventType.ZONE_AND_BIOME_DISCOVERY && "biomeId".equals(dimensionKey)) {
            return "BiomesFound";
        }

        return dimensionKey;
    }

    static String toCanonicalDimensionKey(String eventKey, String storageDimensionKey) {
        if (storageDimensionKey == null || storageDimensionKey.isBlank()) {
            return UNKNOWN;
        }

        GameEventType eventType = parseGameEventType(eventKey);
        String compact = storageDimensionKey.replace("_", "").replace("-", "").toLowerCase();

        if (eventType == GameEventType.BLOCK_BREAK && "inhandbreakdown".equals(compact)) {
            return "itemId";
        }

        if (eventType == GameEventType.DAMAGE && "damagetypebreakdown".equals(compact)) {
            return "causeId";
        }

        if (eventType == GameEventType.DAMAGE && "sourcebreakdown".equals(compact)) {
            return "sourceId";
        }

        if (eventType == GameEventType.ZONE_AND_BIOME_DISCOVERY && "biomesfound".equals(compact)) {
            return "biomeId";
        }

        return storageDimensionKey;
    }
}