package dev.valhal.minecraft.plugin.EventNotifications.core.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerOpEvent extends GameEvent {
    private final UUID playerUuid;
    private final String playerName;

    public PlayerOpEvent(UUID playerUuid, String playerName) {
        super(GameEventType.PLAYER_OP);
        this.playerUuid = playerUuid;
        this.playerName = playerName;
    }

    @Override
    public Map<String, String> getPlaceholders() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player_name", playerName);
        placeholders.put("player_uuid", playerUuid != null ? playerUuid.toString() : "");
        return placeholders;
    }
}
