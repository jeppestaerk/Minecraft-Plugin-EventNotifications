package dev.valhal.minecraft.plugin.EventNotifications.core.event;

import java.util.Map;
import java.util.UUID;

public final class PlayerConnectEvent extends GameEvent {
    private final UUID playerUuid;
    private final String playerName;

    public PlayerConnectEvent(UUID playerUuid, String playerName) {
        super(GameEventType.PLAYER_CONNECT);
        this.playerUuid = playerUuid;
        this.playerName = playerName;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    @Override
    public Map<String, String> getPlaceholders() {
        return Map.of(
                "player_name", playerName,
                "player_uuid", playerUuid.toString()
        );
    }
}
