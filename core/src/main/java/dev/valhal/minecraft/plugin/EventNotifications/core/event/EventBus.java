package dev.valhal.minecraft.plugin.EventNotifications.core.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {
    private final List<EventHandler> handlers = new CopyOnWriteArrayList<>();

    public void subscribe(EventHandler handler) {
        handlers.add(handler);
    }

    public void unsubscribe(EventHandler handler) {
        handlers.remove(handler);
    }

    public void publish(GameEvent event) {
        for (EventHandler handler : handlers) {
            try {
                handler.handle(event);
            } catch (Exception e) {
                System.err.println("Error handling event " + event.getType() + ": " + e.getMessage());
            }
        }
    }
}
