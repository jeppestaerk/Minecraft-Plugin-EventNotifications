package dev.valhal.minecraft.plugin.EventNotifications.core.config;

import java.util.Map;

public record TargetConfig(
        String name,
        String type,
        boolean enabled,
        Map<String, Object> properties
) {
    public String getString(String key) {
        Object value = properties.get(key);
        return value != null ? value.toString() : null;
    }

    public String getString(String key, String defaultValue) {
        Object value = properties.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = properties.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        return defaultValue;
    }
}
