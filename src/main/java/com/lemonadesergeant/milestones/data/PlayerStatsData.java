package com.lemonadesergeant.milestones.data;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.BsonValue;

public class PlayerStatsData implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String UNKNOWN = "unknown";

    private static final MapCodec<Long, Map<String, Long>> LONG_MAP_CODEC = new MapCodec<>(Codec.LONG, LinkedHashMap::new, false);
    private static final MapCodec<Map<String, Long>, Map<String, Map<String, Long>>> BREAKDOWNS_CODEC = new MapCodec<>(LONG_MAP_CODEC, LinkedHashMap::new, false);
    private static final MapCodec<DamageCauseStats, Map<String, DamageCauseStats>> DAMAGE_CAUSE_STATS_CODEC =
        new MapCodec<>(DamageCauseStats.CODEC, LinkedHashMap::new, false);
    private static final MapCodec<DamageEntityStats, Map<String, DamageEntityStats>> DAMAGE_ENTITY_STATS_CODEC =
        new MapCodec<>(DamageEntityStats.CODEC, LinkedHashMap::new, false);
    private static final MapCodec<DamageDirectionStats, Map<String, DamageDirectionStats>> DAMAGE_DIRECTION_STATS_CODEC =
        new MapCodec<>(DamageDirectionStats.CODEC, LinkedHashMap::new, false);
    private static final Codec<PersistedPrimaryStats> PERSISTED_PRIMARY_STATS_CODEC = new PersistedPrimaryStatsCodec();
    private static final MapCodec<PersistedPrimaryStats, Map<String, PersistedPrimaryStats>> PRIMARY_STATS_CODEC =
        new MapCodec<>(PERSISTED_PRIMARY_STATS_CODEC, LinkedHashMap::new, false);
    public static final MapCodec<Map<String, PersistedPrimaryStats>, Map<String, Map<String, PersistedPrimaryStats>>> PERSISTED_STATS_CODEC =
        new MapCodec<>(PRIMARY_STATS_CODEC, LinkedHashMap::new, false);

    private final Map<String, EventAggregate> eventAggregates = new HashMap<>();

    public void record(GameEventType eventType, String primaryValue, Map<String, String> dimensions, long quantity) {
        if (eventType == null) {
            return;
        }

        String eventKey = eventType.name();
        EventAggregate aggregate = eventAggregates.computeIfAbsent(eventKey, ignored -> new EventAggregate());
        aggregate.record(normalize(primaryValue), dimensions == null ? Map.of() : dimensions, quantity);
    }

    public long getTotalOccurrences(GameEventType eventType) {
        EventAggregate aggregate = getAggregate(eventType);
        return aggregate == null ? 0 : aggregate.total.occurrences;
    }

    public long getTotalQuantity(GameEventType eventType) {
        EventAggregate aggregate = getAggregate(eventType);
        return aggregate == null ? 0 : aggregate.total.quantity;
    }

    public long getPrimaryOccurrences(GameEventType eventType, String primaryValue) {
        EventAggregate aggregate = getAggregate(eventType);
        if (aggregate == null) {
            return 0;
        }

        Counter counter = aggregate.byPrimary.get(normalize(primaryValue));
        return counter == null ? 0 : counter.occurrences;
    }

    public long getPrimaryQuantity(GameEventType eventType, String primaryValue) {
        EventAggregate aggregate = getAggregate(eventType);
        if (aggregate == null) {
            return 0;
        }

        Counter counter = aggregate.byPrimary.get(normalize(primaryValue));
        return counter == null ? 0 : counter.quantity;
    }

    public long getSplitOccurrences(GameEventType eventType, String primaryValue, String dimensionName, String dimensionValue) {
        Counter counter = getSplitCounter(eventType, primaryValue, dimensionName, dimensionValue);
        return counter == null ? 0 : counter.occurrences;
    }

    public long getSplitQuantity(GameEventType eventType, String primaryValue, String dimensionName, String dimensionValue) {
        Counter counter = getSplitCounter(eventType, primaryValue, dimensionName, dimensionValue);
        return counter == null ? 0 : counter.quantity;
    }

    public Map<String, Long> getPrimaryOccurrenceBreakdown(GameEventType eventType) {
        EventAggregate aggregate = getAggregate(eventType);
        if (aggregate == null) {
            return Map.of();
        }

        Map<String, Long> result = new HashMap<>();
        aggregate.byPrimary.forEach((primary, counter) -> result.put(primary, counter.occurrences));
        return Collections.unmodifiableMap(result);
    }

    public Map<String, Long> getSplitOccurrenceBreakdown(GameEventType eventType, String primaryValue, String dimensionName) {
        EventAggregate aggregate = getAggregate(eventType);
        if (aggregate == null) {
            return Map.of();
        }

        Map<String, Map<String, Counter>> byDimension = aggregate.splitByPrimary.get(normalize(primaryValue));
        if (byDimension == null) {
            return Map.of();
        }

        Map<String, Counter> counters = byDimension.get(normalize(dimensionName));
        if (counters == null) {
            return Map.of();
        }

        Map<String, Long> result = new HashMap<>();
        counters.forEach((value, counter) -> result.put(value, counter.occurrences));
        return Collections.unmodifiableMap(result);
    }

    public long getBlockBreakTotal(String blockId) {
        return getPrimaryOccurrences(GameEventType.BLOCK_BREAK, blockId);
    }

    public Map<String, Long> getBlockBreakByHeldItem(String blockId) {
        return getSplitOccurrenceBreakdown(GameEventType.BLOCK_BREAK, blockId, "itemId");
    }

    public Map<String, Set<String>> getZoneBiomeDiscoveryMap() {
        Map<String, Set<String>> zoneBiomes = new LinkedHashMap<>();
        Map<String, Long> zones = getPrimaryOccurrenceBreakdown(GameEventType.ZONE_AND_BIOME_DISCOVERY);

        for (String zoneId : zones.keySet()) {
            Map<String, Long> biomeBreakdown = getSplitOccurrenceBreakdown(GameEventType.ZONE_AND_BIOME_DISCOVERY, zoneId, "biomeId");
            Set<String> biomes = new LinkedHashSet<>();
            for (String biomeId : biomeBreakdown.keySet()) {
                if (!UNKNOWN.equalsIgnoreCase(biomeId)) {
                    biomes.add(biomeId);
                }
            }
            zoneBiomes.put(zoneId, Collections.unmodifiableSet(biomes));
        }

        return Collections.unmodifiableMap(zoneBiomes);
    }

    public Map<String, Map<String, PersistedPrimaryStats>> toPersistedStatsMap() {
        Map<String, Map<String, PersistedPrimaryStats>> stats = new LinkedHashMap<>();

        eventAggregates.forEach((eventKey, aggregate) -> {
            GameEventType eventType = EventStatsKeyMapper.parseGameEventType(eventKey);
            Map<String, PersistedPrimaryStats> byPrimary = new LinkedHashMap<>();

            aggregate.byPrimary.forEach((primaryValue, counter) -> {
                Map<String, Map<String, Counter>> dimensions = aggregate.splitByPrimary.get(primaryValue);

                if (eventType == GameEventType.DAMAGE) {
                    byPrimary.put(primaryValue, PersistedPrimaryStats.asDamage(counter.quantity, buildDamageDirections(dimensions)));
                    return;
                }

                Map<String, Map<String, Long>> breakdowns = new LinkedHashMap<>();
                if (dimensions != null) {
                    dimensions.forEach((dimensionName, byValue) -> {
                        Map<String, Long> values = new LinkedHashMap<>();
                        byValue.forEach((dimensionValue, dimensionCounter) -> values.put(dimensionValue, dimensionCounter.occurrences));
                        breakdowns.put(EventStatsKeyMapper.toStorageDimensionKey(eventKey, dimensionName), values);
                    });
                }

                if (eventType == GameEventType.BLOCK_PLACE) {
                    byPrimary.put(primaryValue, PersistedPrimaryStats.asScalarCount(counter.occurrences));
                    return;
                }

                boolean includeQuantity = switch (eventType) {
                    case DAMAGE, CRAFT_RECIPE, INTERACTIVELY_PICKUP, ITEM_PICKUP -> true;
                    default -> false;
                };

                byPrimary.put(primaryValue, new PersistedPrimaryStats(counter.occurrences, includeQuantity ? counter.quantity : 0, breakdowns));
            });

            stats.put(EventStatsKeyMapper.toStorageEventKey(eventKey), byPrimary);
        });

        return stats;
    }

    public static PlayerStatsData fromPersistedStatsMap(Map<String, Map<String, PersistedPrimaryStats>> statsMap) {
        PlayerStatsData data = new PlayerStatsData();

        if (statsMap == null || statsMap.isEmpty()) {
            return data;
        }

        for (Map.Entry<String, Map<String, PersistedPrimaryStats>> eventEntry : statsMap.entrySet()) {
            String eventKey = EventStatsKeyMapper.toCanonicalEventKey(eventEntry.getKey());
            GameEventType eventType = EventStatsKeyMapper.parseGameEventType(eventKey);
            Map<String, PersistedPrimaryStats> byPrimary = eventEntry.getValue();

            if (byPrimary == null || byPrimary.isEmpty()) {
                continue;
            }

            EventAggregate aggregate = data.eventAggregates.computeIfAbsent(eventKey, ignored -> new EventAggregate());
            for (Map.Entry<String, PersistedPrimaryStats> primaryEntry : byPrimary.entrySet()) {
                PersistedPrimaryStats persistedPrimary = primaryEntry.getValue();
                if (persistedPrimary == null) {
                    continue;
                }

                String normalizedPrimary = normalize(primaryEntry.getKey());

                if (eventType == GameEventType.DAMAGE && persistedPrimary.damageDirections != null && !persistedPrimary.damageDirections.isEmpty()) {
                    long totalDamage = Math.max(0, persistedPrimary.total);
                    if (totalDamage <= 0) {
                        totalDamage = DamageStatsTotals.estimateDamageTotal(persistedPrimary.damageDirections);
                    }

                    addPrimaryCounter(aggregate, normalizedPrimary, totalDamage, totalDamage);
                    Map<String, Map<String, Counter>> byDimension = aggregate.splitByPrimary.computeIfAbsent(normalizedPrimary, ignored -> new HashMap<>());

                    persistedPrimary.damageDirections.forEach((direction, directionStats) -> {
                        if (directionStats == null || directionStats.breakdowns == null || directionStats.breakdowns.isEmpty()) {
                            return;
                        }

                        directionStats.breakdowns.forEach((entityKey, entityStats) -> {
                            if (entityStats == null || entityStats.damageBreakdown == null || entityStats.damageBreakdown.isEmpty()) {
                                return;
                            }

                            entityStats.damageBreakdown.forEach((causeKey, causeStats) -> {
                                if (causeStats == null || causeStats.inHandDamageBreakdown == null || causeStats.inHandDamageBreakdown.isEmpty()) {
                                    return;
                                }

                                causeStats.inHandDamageBreakdown.forEach((inHandKey, amount) -> {
                                    long normalizedAmount = Math.max(0, amount == null ? 0 : amount);
                                    if (normalizedAmount <= 0) {
                                        return;
                                    }

                                    String path = DamageStatsLayout.buildDamagePath(
                                        normalize(direction),
                                        normalize(entityKey),
                                        normalize(causeKey),
                                        normalize(inHandKey)
                                    );
                                    Map<String, Counter> pathValues = byDimension.computeIfAbsent(DamageStatsLayout.DIMENSION_DAMAGE_PATH, ignored -> new HashMap<>());
                                    Counter existingPath = pathValues.get(path);
                                    if (existingPath == null) {
                                        pathValues.put(path, new Counter(normalizedAmount, normalizedAmount));
                                    } else {
                                        existingPath.occurrences += normalizedAmount;
                                        existingPath.quantity += normalizedAmount;
                                    }
                                });
                            });
                        });
                    });

                    continue;
                }

                if (eventType == GameEventType.BLOCK_PLACE && UNKNOWN.equalsIgnoreCase(normalizedPrimary)) {
                    Map<String, Long> itemInHandBreakdown = firstNonEmptyBreakdown(
                        persistedPrimary.breakdowns,
                        "itemInHand",
                        "InHandBreakDown"
                    );
                    if (!itemInHandBreakdown.isEmpty()) {
                        itemInHandBreakdown.forEach((itemId, count) -> {
                            long normalizedCount = Math.max(0, count == null ? 0 : count);
                            addPrimaryCounter(aggregate, normalize(itemId), normalizedCount, normalizedCount);
                        });
                        continue;
                    }
                }

                long occurrences = Math.max(0, persistedPrimary.total);
                long quantity = Math.max(0, persistedPrimary.quantity <= 0 ? occurrences : persistedPrimary.quantity);

                addPrimaryCounter(aggregate, normalizedPrimary, occurrences, quantity);

                if (persistedPrimary.breakdowns == null || persistedPrimary.breakdowns.isEmpty()) {
                    continue;
                }

                Map<String, Map<String, Counter>> byDimension = aggregate.splitByPrimary.computeIfAbsent(normalizedPrimary, ignored -> new HashMap<>());
                persistedPrimary.breakdowns.forEach((storageDimensionName, storageValues) -> {
                    String normalizedDimension = normalize(EventStatsKeyMapper.toCanonicalDimensionKey(eventKey, storageDimensionName));

                    if (eventType == GameEventType.ZONE_AND_BIOME_DISCOVERY
                        && ("biomeDisplayName".equalsIgnoreCase(normalizedDimension) || "major".equalsIgnoreCase(normalizedDimension))) {
                        return;
                    }

                    if (eventType == GameEventType.BLOCK_USE && "itemId".equalsIgnoreCase(normalizedDimension)) {
                        return;
                    }

                    Map<String, Counter> byValue = byDimension.computeIfAbsent(normalizedDimension, ignored -> new HashMap<>());

                    if (storageValues == null || storageValues.isEmpty()) {
                        return;
                    }

                    storageValues.forEach((dimensionValue, occurrenceValue) -> {
                        long normalizedOccurrence = Math.max(0, occurrenceValue == null ? 0 : occurrenceValue);
                        byValue.put(normalize(dimensionValue), new Counter(normalizedOccurrence, normalizedOccurrence));
                    });
                });
            }
        }

        return data;
    }

    private static void addPrimaryCounter(EventAggregate aggregate, String primary, long occurrences, long quantity) {
        Counter existing = aggregate.byPrimary.get(primary);
        if (existing == null) {
            existing = new Counter(occurrences, quantity);
            aggregate.byPrimary.put(primary, existing);
        } else {
            existing.occurrences += occurrences;
            existing.quantity += quantity;
        }

        aggregate.total.occurrences += occurrences;
        aggregate.total.quantity += quantity;
    }

    private static Map<String, Long> firstNonEmptyBreakdown(Map<String, Map<String, Long>> breakdowns, String... keys) {
        if (breakdowns == null || breakdowns.isEmpty()) {
            return Map.of();
        }

        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }

            Map<String, Long> values = breakdowns.get(key);
            if (values == null) {
                values = breakdowns.get(key.toLowerCase());
            }
            if (values != null && !values.isEmpty()) {
                return values;
            }
        }

        return Map.of();
    }

    private static Map<String, DamageDirectionStats> buildDamageDirections(Map<String, Map<String, Counter>> byDimension) {
        if (byDimension == null || byDimension.isEmpty()) {
            return Map.of();
        }

        Map<String, Counter> paths = byDimension.get(DamageStatsLayout.DIMENSION_DAMAGE_PATH);
        if (paths == null || paths.isEmpty()) {
            return Map.of();
        }

        Map<String, DamageDirectionStats> directions = new LinkedHashMap<>();
        paths.forEach((path, counter) -> {
            if (path == null || path.isBlank() || counter == null) {
                return;
            }

            DamageStatsLayout.DamagePathParts parts = DamageStatsLayout.parseDamagePath(path);
            String directionKey = normalize(DamageStatsLayout.normalizeDirection(parts.direction));
            String entityKey = normalize(parts.counterparty);
            String causeKey = normalize(parts.causeId);
            String inHandKey = normalize(parts.inHandId);
            long amount = Math.max(0, counter.quantity);

            if (amount <= 0) {
                return;
            }

            DamageDirectionStats direction = directions.computeIfAbsent(directionKey, ignored -> new DamageDirectionStats());
            direction.total += amount;

            DamageEntityStats entity = direction.breakdowns.computeIfAbsent(entityKey, ignored -> new DamageEntityStats());
            entity.total += amount;

            DamageCauseStats cause = entity.damageBreakdown.computeIfAbsent(causeKey, ignored -> new DamageCauseStats());
            cause.total += amount;
            cause.inHandDamageBreakdown.merge(inHandKey, amount, Long::sum);
        });

        return directions;
    }

    private Counter getSplitCounter(GameEventType eventType, String primaryValue, String dimensionName, String dimensionValue) {
        EventAggregate aggregate = getAggregate(eventType);
        if (aggregate == null) {
            return null;
        }

        Map<String, Map<String, Counter>> byDimension = aggregate.splitByPrimary.get(normalize(primaryValue));
        if (byDimension == null) {
            return null;
        }

        Map<String, Counter> byDimensionValue = byDimension.get(normalize(dimensionName));
        if (byDimensionValue == null) {
            return null;
        }

        return byDimensionValue.get(normalize(dimensionValue));
    }

    private EventAggregate getAggregate(GameEventType eventType) {
        if (eventType == null) {
            return null;
        }

        return eventAggregates.get(eventType.name());
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }

        return value;
    }

    private static final class EventAggregate implements Serializable {

        private static final long serialVersionUID = 1L;

        private final Counter total = new Counter();
        private final Map<String, Counter> byPrimary = new HashMap<>();
        private final Map<String, Map<String, Map<String, Counter>>> splitByPrimary = new HashMap<>();

        private void record(String primaryValue, Map<String, String> dimensions, long quantity) {
            long normalizedQuantity = Math.max(0, quantity);
            total.increment(normalizedQuantity);

            Counter primaryCounter = byPrimary.computeIfAbsent(primaryValue, ignored -> new Counter());
            primaryCounter.increment(normalizedQuantity);

            Map<String, Map<String, Counter>> byDimension = splitByPrimary.computeIfAbsent(primaryValue, ignored -> new HashMap<>());
            dimensions.forEach((name, value) -> {
                String dimensionName = normalize(name);
                String dimensionValue = normalize(value);
                Map<String, Counter> byValue = byDimension.computeIfAbsent(dimensionName, ignored -> new HashMap<>());
                Counter counter = byValue.computeIfAbsent(dimensionValue, ignored -> new Counter());
                counter.increment(normalizedQuantity);
            });
        }
    }

    private static final class Counter implements Serializable {

        private static final long serialVersionUID = 1L;

        private long occurrences;
        private long quantity;

        private Counter() {
        }

        private Counter(long occurrences, long quantity) {
            this.occurrences = occurrences;
            this.quantity = quantity;
        }

        private void increment(long quantityValue) {
            occurrences += 1;
            quantity += quantityValue;
        }
    }

    public static class PersistedPrimaryStats {

        private static final BuilderCodec<PersistedPrimaryStats> STRUCTURED_CODEC = BuilderCodec.builder(PersistedPrimaryStats.class, PersistedPrimaryStats::new)
            .append(new KeyedCodec<>("Total", Codec.LONG), (stats, value) -> stats.total = value == null ? 0 : value, stats -> stats.total)
            .add()
            .append(new KeyedCodec<>("Quantity", Codec.LONG), (stats, value) -> stats.quantity = value == null ? 0 : value, stats -> stats.quantity)
            .add()
            .append(new KeyedCodec<>("Breakdowns", BREAKDOWNS_CODEC), (stats, value) -> stats.breakdowns = value == null ? new LinkedHashMap<>() : value, stats -> stats.breakdowns)
            .add()
            .build();

        private long total;
        private long quantity;
        private Map<String, Map<String, Long>> breakdowns = new LinkedHashMap<>();
        private Map<String, DamageDirectionStats> damageDirections = new LinkedHashMap<>();
        private boolean scalarCount;

        public PersistedPrimaryStats() {
        }

        public PersistedPrimaryStats(long total, long quantity, Map<String, Map<String, Long>> breakdowns) {
            this.total = Math.max(0, total);
            this.quantity = Math.max(0, quantity);
            this.breakdowns = breakdowns == null ? new LinkedHashMap<>() : breakdowns;
        }

        public static PersistedPrimaryStats asScalarCount(long total) {
            PersistedPrimaryStats stats = new PersistedPrimaryStats(total, 0, Map.of());
            stats.scalarCount = true;
            return stats;
        }

        public static PersistedPrimaryStats asDamage(long total, Map<String, DamageDirectionStats> directions) {
            PersistedPrimaryStats stats = new PersistedPrimaryStats();
            stats.total = Math.max(0, total);
            stats.damageDirections = directions == null ? new LinkedHashMap<>() : directions;
            return stats;
        }

        private boolean isScalarCount() {
            return scalarCount && (breakdowns == null || breakdowns.isEmpty()) && quantity <= 0;
        }

        private boolean hasDamageDirections() {
            return damageDirections != null && !damageDirections.isEmpty();
        }
    }

    public static class DamageDirectionStats {

        public static final BuilderCodec<DamageDirectionStats> CODEC = BuilderCodec.builder(DamageDirectionStats.class, DamageDirectionStats::new)
            .append(new KeyedCodec<>("Total", Codec.LONG), (stats, value) -> stats.total = value == null ? 0 : value, stats -> stats.total)
            .add()
            .append(new KeyedCodec<>("Breakdowns", DAMAGE_ENTITY_STATS_CODEC), (stats, value) -> stats.breakdowns = value == null ? new LinkedHashMap<>() : value, stats -> stats.breakdowns)
            .add()
            .build();

        long total;
        private Map<String, DamageEntityStats> breakdowns = new LinkedHashMap<>();
    }

    public static class DamageEntityStats {

        public static final BuilderCodec<DamageEntityStats> CODEC = BuilderCodec.builder(DamageEntityStats.class, DamageEntityStats::new)
            .append(new KeyedCodec<>("Total", Codec.LONG), (stats, value) -> stats.total = value == null ? 0 : value, stats -> stats.total)
            .add()
            .append(new KeyedCodec<>("DamageBreakdown", DAMAGE_CAUSE_STATS_CODEC), (stats, value) -> stats.damageBreakdown = value == null ? new LinkedHashMap<>() : value, stats -> stats.damageBreakdown)
            .add()
            .build();

        private long total;
        private Map<String, DamageCauseStats> damageBreakdown = new LinkedHashMap<>();
    }

    public static class DamageCauseStats {

        public static final BuilderCodec<DamageCauseStats> CODEC = BuilderCodec.builder(DamageCauseStats.class, DamageCauseStats::new)
            .append(new KeyedCodec<>("Total", Codec.LONG), (stats, value) -> stats.total = value == null ? 0 : value, stats -> stats.total)
            .add()
            .append(new KeyedCodec<>("InHandDamageBreakdown", LONG_MAP_CODEC), (stats, value) -> stats.inHandDamageBreakdown = value == null ? new LinkedHashMap<>() : value, stats -> stats.inHandDamageBreakdown)
            .add()
            .build();

        private long total;
        private Map<String, Long> inHandDamageBreakdown = new LinkedHashMap<>();
    }

    private static final class PersistedPrimaryStatsCodec implements Codec<PersistedPrimaryStats> {

        @Override
        public PersistedPrimaryStats decode(BsonValue bsonValue, com.hypixel.hytale.codec.ExtraInfo extraInfo) {
            if (bsonValue == null || bsonValue.isNull()) {
                return new PersistedPrimaryStats();
            }

            if (bsonValue.isNumber()) {
                return PersistedPrimaryStats.asScalarCount(Math.max(0, bsonValue.asNumber().longValue()));
            }

            if (bsonValue.isDocument()) {
                BsonDocument document = bsonValue.asDocument();
                if (document.containsKey(DamageStatsLayout.DIRECTION_DEALT) || document.containsKey(DamageStatsLayout.DIRECTION_TAKEN)) {
                    PersistedPrimaryStats damageStats = new PersistedPrimaryStats();
                    if (document.containsKey(DamageStatsLayout.DIRECTION_DEALT)) {
                        DamageDirectionStats dealt = DamageDirectionStats.CODEC.decode(document.get(DamageStatsLayout.DIRECTION_DEALT), extraInfo);
                        if (dealt != null) {
                            damageStats.damageDirections.put(DamageStatsLayout.DIRECTION_DEALT, dealt);
                            damageStats.total += Math.max(0, dealt.total);
                        }
                    }
                    if (document.containsKey(DamageStatsLayout.DIRECTION_TAKEN)) {
                        DamageDirectionStats taken = DamageDirectionStats.CODEC.decode(document.get(DamageStatsLayout.DIRECTION_TAKEN), extraInfo);
                        if (taken != null) {
                            damageStats.damageDirections.put(DamageStatsLayout.DIRECTION_TAKEN, taken);
                            damageStats.total += Math.max(0, taken.total);
                        }
                    }
                    return damageStats;
                }
            }

            PersistedPrimaryStats decoded = PersistedPrimaryStats.STRUCTURED_CODEC.decode(bsonValue, extraInfo);
            if (decoded == null) {
                return new PersistedPrimaryStats();
            }

            decoded.total = Math.max(0, decoded.total);
            decoded.quantity = Math.max(0, decoded.quantity);
            if (decoded.breakdowns == null) {
                decoded.breakdowns = new LinkedHashMap<>();
            }
            if (decoded.damageDirections == null) {
                decoded.damageDirections = new LinkedHashMap<>();
            }
            return decoded;
        }

        @Override
        public BsonValue encode(PersistedPrimaryStats stats, com.hypixel.hytale.codec.ExtraInfo extraInfo) {
            PersistedPrimaryStats value = stats == null ? new PersistedPrimaryStats() : stats;

            if (value.isScalarCount()) {
                return new BsonInt64(Math.max(0, value.total));
            }

            if (value.hasDamageDirections()) {
                BsonDocument damageDocument = new BsonDocument();
                DamageDirectionStats dealt = value.damageDirections.get(DamageStatsLayout.DIRECTION_DEALT);
                DamageDirectionStats taken = value.damageDirections.get(DamageStatsLayout.DIRECTION_TAKEN);

                if (dealt != null) {
                    damageDocument.put(DamageStatsLayout.DIRECTION_DEALT, DamageDirectionStats.CODEC.encode(dealt, extraInfo));
                }
                if (taken != null) {
                    damageDocument.put(DamageStatsLayout.DIRECTION_TAKEN, DamageDirectionStats.CODEC.encode(taken, extraInfo));
                }

                return damageDocument;
            }

            BsonDocument document = new BsonDocument();
            document.put("Total", new BsonInt64(Math.max(0, value.total)));

            if (value.quantity > 0 && value.quantity != value.total) {
                document.put("Quantity", new BsonInt64(value.quantity));
            }

            if (value.breakdowns != null && !value.breakdowns.isEmpty()) {
                BsonValue breakdownsValue = BREAKDOWNS_CODEC.encode(value.breakdowns, extraInfo);
                if (breakdownsValue != null && breakdownsValue.isDocument() && !breakdownsValue.asDocument().isEmpty()) {
                    document.put("Breakdowns", breakdownsValue);
                }
            }

            return document;
        }

        @Override
        public Schema toSchema(SchemaContext context) {
            return context.refDefinition(PersistedPrimaryStats.STRUCTURED_CODEC);
        }
    }
}