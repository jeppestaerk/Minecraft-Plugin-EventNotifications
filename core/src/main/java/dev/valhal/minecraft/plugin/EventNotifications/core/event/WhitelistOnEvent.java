package dev.valhal.minecraft.plugin.EventNotifications.core.event;

import java.util.HashMap;
import java.util.Map;

public final class WhitelistOnEvent extends GameEvent {

    public WhitelistOnEvent() {
        super(GameEventType.SERVER_WHITELIST_ON);
    }

    @Override
    public Map<String, String> getPlaceholders() {
        return new HashMap<>();
    }
}
