package dev.valhal.minecraft.plugin.EventNotifications.core.event;

import java.util.Map;

public final class ServerShutdownEvent extends GameEvent {

    public ServerShutdownEvent() {
        super(GameEventType.SERVER_SHUTDOWN);
    }

    @Override
    public Map<String, String> getPlaceholders() {
        return Map.of();
    }
}
