package dev.valhal.minecraft.plugin.EventNotifications.core.event;

import java.util.Map;
import java.util.UUID;

public final class PlayerKickedEvent extends GameEvent {
    private final UUID playerUuid;
    private final String playerName;
    private final String reason;

    public PlayerKickedEvent(UUID playerUuid, String playerName, String reason) {
        super(GameEventType.PLAYER_KICKED);
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.reason = reason != null ? reason : "";
    }

    @Override
    public Map<String, String> getPlaceholders() {
        return Map.of(
                "player_name", playerName,
                "player_uuid", playerUuid.toString(),
                "reason", reason
        );
    }
}
