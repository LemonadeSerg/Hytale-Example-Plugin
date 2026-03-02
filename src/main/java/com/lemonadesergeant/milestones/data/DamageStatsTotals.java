package com.lemonadesergeant.milestones.data;

import java.util.Map;

final class DamageStatsTotals {

    private DamageStatsTotals() {
    }

    static long estimateDamageTotal(Map<String, PlayerStatsData.DamageDirectionStats> damageDirections) {
        if (damageDirections == null || damageDirections.isEmpty()) {
            return 0;
        }

        long total = 0;
        for (PlayerStatsData.DamageDirectionStats stats : damageDirections.values()) {
            if (stats == null) {
                continue;
            }
            total += Math.max(0, stats.total);
        }

        return total;
    }
}