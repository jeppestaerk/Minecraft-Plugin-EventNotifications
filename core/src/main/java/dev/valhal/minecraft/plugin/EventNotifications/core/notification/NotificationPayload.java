package dev.valhal.minecraft.plugin.EventNotifications.core.notification;

import java.util.Collections;
import java.util.Map;

public record NotificationPayload(
        String title,
        String message,
        Map<String, Object> extras
) {
    public NotificationPayload(String title, String message) {
        this(title, message, Collections.emptyMap());
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

    public NotificationPriority getPriority() {
        String priorityStr = getString("priority", "default");
        return NotificationPriority.fromString(priorityStr);
    }
}
