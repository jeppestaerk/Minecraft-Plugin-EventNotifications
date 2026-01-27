package dev.valhal.minecraft.plugin.EventNotifications.core.event;

@FunctionalInterface
public interface EventHandler {
    void handle(GameEvent event);
}
