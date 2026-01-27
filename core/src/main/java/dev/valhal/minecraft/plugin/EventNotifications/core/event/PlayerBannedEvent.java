package dev.valhal.minecraft.plugin.EventNotifications.core.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerBannedEvent extends GameEvent {
    private final UUID playerUuid;
    private final String playerName;
    private final String reason;
    private final String bannedBy;

    public PlayerBannedEvent(UUID playerUuid, String playerName, String reason, String bannedBy) {
        super(GameEventType.PLAYER_BANNED);
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.reason = reason != null ? reason : "";
        this.bannedBy = bannedBy != null ? bannedBy : "Server";
    }

    @Override
    public Map<String, String> getPlaceholders() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player_name", playerName);
        placeholders.put("player_uuid", playerUuid != null ? playerUuid.toString() : "");
        placeholders.put("reason", reason);
        placeholders.put("banned_by", bannedBy);
        return placeholders;
    }
}
