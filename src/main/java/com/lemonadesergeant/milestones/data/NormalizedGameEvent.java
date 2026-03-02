package com.lemonadesergeant.milestones.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NormalizedGameEvent {

    private final GameEventType type;
    private final long timestamp;
    private final Map<String, Object> fields = new LinkedHashMap<>();

    private NormalizedGameEvent(GameEventType type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public static NormalizedGameEvent of(GameEventType type) {
        return new NormalizedGameEvent(type);
    }

    public NormalizedGameEvent put(String key, Object value) {
        fields.put(key, value);
        return this;
    }

    public GameEventType getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getFields() {
        return Collections.unmodifiableMap(fields);
    }

    public Object get(String key) {
        return fields.get(key);
    }

    public String getString(String key) {
        Object value = fields.get(key);
        if (value == null) {
            return null;
        }

        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    public Long getLong(String key) {
        Object value = fields.get(key);
        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    public Boolean getBoolean(String key) {
        Object value = fields.get(key);
        if (value == null) {
            return null;
        }

        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        return Boolean.parseBoolean(String.valueOf(value));
    }
}