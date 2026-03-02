package com.lemonadesergeant.milestones.managers;

import com.lemonadesergeant.milestones.data.NormalizedGameEvent;
import com.lemonadesergeant.milestones.logging.LogSource;

final class EventLogSourceResolver {

    private EventLogSourceResolver() {
    }

    static LogSource resolve(NormalizedGameEvent event, LogSource fallback) {
        if (event == null || event.getType() == null) {
            return fallback;
        }

        try {
            return LogSource.valueOf(event.getType().name());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}