package dev.valhal.minecraft.plugin.EventNotifications.core.event;

import java.util.HashMap;
import java.util.Map;

public final class WhitelistOffEvent extends GameEvent {

    public WhitelistOffEvent() {
        super(GameEventType.SERVER_WHITELIST_OFF);
    }

    @Override
    public Map<String, String> getPlaceholders() {
        return new HashMap<>();
    }
}
