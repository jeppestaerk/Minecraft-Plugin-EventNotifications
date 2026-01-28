package dev.valhal.minecraft.plugin.EventNotifications.core.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record TargetConfig(
        String name,
        String type,
        boolean enabled,
        Map<String, Object> properties
) {
    // Property definitions per target type
    private static final Map<String, List<String>> PROPERTIES_BY_TYPE = Map.of(
            "ntfy", List.of("server", "topic", "markdown", "icon", "auth_token"),
            "discord", List.of("webhook_url", "use_embeds", "icon"),
            "slack", List.of("webhook_url", "use_attachments", "icon"),
            "webhook", List.of("url", "method", "icon", "auth_token")
    );

    // Common properties applicable to all types
    private static final List<String> COMMON_PROPERTIES = List.of("enabled", "icon");

    /**
     * Returns a new TargetConfig with the enabled flag changed.
     */
    public TargetConfig withEnabled(boolean enabled) {
        return new TargetConfig(name, type, enabled, properties);
    }

    /**
     * Returns a new TargetConfig with the specified property set.
     */
    public TargetConfig withProperty(String key, Object value) {
        Map<String, Object> newProperties = new HashMap<>(properties);
        newProperties.put(key, value);
        return new TargetConfig(name, type, enabled, newProperties);
    }

    /**
     * Returns the list of valid properties for this target's type.
     */
    public List<String> getValidProperties() {
        return PROPERTIES_BY_TYPE.getOrDefault(type.toLowerCase(), List.of());
    }

    /**
     * Returns the list of valid properties for a given target type.
     */
    public static List<String> getPropertiesForType(String type) {
        return PROPERTIES_BY_TYPE.getOrDefault(type.toLowerCase(), List.of());
    }

    /**
     * Returns all known target types.
     */
    public static Set<String> getKnownTypes() {
        return PROPERTIES_BY_TYPE.keySet();
    }

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
