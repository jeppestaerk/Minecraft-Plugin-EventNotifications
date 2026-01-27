package dev.valhal.minecraft.plugin.EventNotifications.core.event;

import java.util.Map;

public final class ServerStartupEvent extends GameEvent {

    public ServerStartupEvent() {
        super(GameEventType.SERVER_STARTUP);
    }

    @Override
    public Map<String, String> getPlaceholders() {
        return Map.of();
    }
}
