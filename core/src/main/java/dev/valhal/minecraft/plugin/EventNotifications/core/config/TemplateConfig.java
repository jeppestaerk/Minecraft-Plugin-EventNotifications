package dev.valhal.minecraft.plugin.EventNotifications.core.config;

import dev.valhal.minecraft.plugin.EventNotifications.core.event.GameEventType;
import dev.valhal.minecraft.plugin.EventNotifications.core.notification.NotificationPriority;

import java.util.Collections;
import java.util.Map;

public record TemplateConfig(
        Map<GameEventType, EventTemplate> templates
) {
    public EventTemplate getTemplate(GameEventType type) {
        return templates.get(type);
    }

    public boolean hasTemplate(GameEventType type) {
        return templates.containsKey(type);
    }

    public record EventTemplate(
            boolean enabled,
            String title,
            String message,
            Map<String, Object> extras
    ) {
        public EventTemplate(boolean enabled, String title, String message) {
            this(enabled, title, message, Collections.emptyMap());
        }

        public static EventTemplate disabled() {
            return new EventTemplate(false, "", "", Collections.emptyMap());
        }

        public String getString(String key) {
            Object value = extras.get(key);
            return value != null ? value.toString() : null;
        }

        public String getString(String key, String defaultValue) {
            Object value = extras.get(key);
            return value != null ? value.toString() : defaultValue;
        }

        public boolean getBoolean(String key, boolean defaultValue) {
            Object value = extras.get(key);
            if (value instanceof Boolean b) {
                return b;
            }
            if (value instanceof String s) {
                return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes") || s.equals("1");
            }
            return defaultValue;
        }

        public NotificationPriority priority() {
            String priorityStr = getString("priority", "default");
            return NotificationPriority.fromString(priorityStr);
        }
    }
}
