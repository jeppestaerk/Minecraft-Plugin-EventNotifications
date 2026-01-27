package dev.valhal.minecraft.plugin.EventNotifications.core.notification;

public record NotificationResult(
        boolean success,
        String targetName,
        String message
) {
    public static NotificationResult success(String targetName) {
        return new NotificationResult(true, targetName, "Notification sent successfully");
    }

    public static NotificationResult failure(String targetName, String error) {
        return new NotificationResult(false, targetName, error);
    }
}
