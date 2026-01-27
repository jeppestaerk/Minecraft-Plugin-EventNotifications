package dev.valhal.minecraft.plugin.EventNotifications.core.notification;

import java.util.concurrent.CompletableFuture;

public interface NotificationTarget {
    String getName();

    String getType();

    boolean isEnabled();

    CompletableFuture<NotificationResult> send(NotificationPayload payload);
}
