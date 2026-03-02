package com.lemonadesergeant.milestones.data;

/**
 * Shared constants and helpers for damage stats encoding/decoding.
 * Keeping this in one place ensures parity between event capture and persistence shape.
 */
public final class DamageStatsLayout {

    public static final String DIMENSION_DAMAGE_PATH = "damagePath";
    public static final String DIRECTION_DEALT = "Dealt";
    public static final String DIRECTION_TAKEN = "Taken";
    public static final String UNKNOWN = "unknown";
    public static final String UNARMED = "unarmed";

    private DamageStatsLayout() {
    }

    public static String normalizeDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return DIRECTION_TAKEN;
        }

        if (DIRECTION_DEALT.equalsIgnoreCase(direction)) {
            return DIRECTION_DEALT;
        }

        if (DIRECTION_TAKEN.equalsIgnoreCase(direction)) {
            return DIRECTION_TAKEN;
        }

        return direction;
    }

    public static String withDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static String buildDamagePath(String direction, String counterparty, String causeId, String inHandId) {
        return withDefault(direction, DIRECTION_TAKEN)
            + "|" + withDefault(counterparty, UNKNOWN)
            + "|" + withDefault(causeId, UNKNOWN)
            + "|" + withDefault(inHandId, UNARMED);
    }

    public static DamagePathParts parseDamagePath(String path) {
        if (path == null || path.isBlank()) {
            return new DamagePathParts(DIRECTION_TAKEN, UNKNOWN, UNKNOWN, UNARMED);
        }

        String[] parts = path.split("\\|", 4);
        String direction = parts.length > 0 ? parts[0] : DIRECTION_TAKEN;
        String counterparty = parts.length > 1 ? parts[1] : UNKNOWN;
        String causeId = parts.length > 2 ? parts[2] : UNKNOWN;
        String inHandId = parts.length > 3 ? parts[3] : UNARMED;

        return new DamagePathParts(direction, counterparty, causeId, inHandId);
    }

    public static final class DamagePathParts {
        public final String direction;
        public final String counterparty;
        public final String causeId;
        public final String inHandId;

        private DamagePathParts(String direction, String counterparty, String causeId, String inHandId) {
            this.direction = direction;
            this.counterparty = counterparty;
            this.causeId = causeId;
            this.inHandId = inHandId;
        }
    }
}