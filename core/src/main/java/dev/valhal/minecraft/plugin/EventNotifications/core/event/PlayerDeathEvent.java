package dev.valhal.minecraft.plugin.EventNotifications.core.event;

import java.util.Map;
import java.util.UUID;

public final class PlayerDeathEvent extends GameEvent {
    private final UUID playerUuid;
    private final String playerName;
    private final String deathMessage;

    public PlayerDeathEvent(UUID playerUuid, String playerName, String deathMessage) {
        super(GameEventType.PLAYER_DEATH);
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.deathMessage = deathMessage;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getDeathMessage() {
        return deathMessage;
    }

    @Override
    public Map<String, String> getPlaceholders() {
        return Map.of(
                "player_name", playerName,
                "player_uuid", playerUuid.toString(),
                "death_message", deathMessage
        );
    }
}
