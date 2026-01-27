package dev.valhal.minecraft.plugin.EventNotifications.core.notification;

public enum NotificationPriority {
    MIN(1),
    LOW(2),
    DEFAULT(3),
    HIGH(4),
    URGENT(5);

    private final int level;

    NotificationPriority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public static NotificationPriority fromString(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        return switch (value.toLowerCase()) {
            case "min", "1" -> MIN;
            case "low", "2" -> LOW;
            case "high", "4" -> HIGH;
            case "urgent", "max", "5" -> URGENT;
            default -> DEFAULT;
        };
    }
}
