package dev.valhal.minecraft.plugin.EventNotifications.core.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerAdvancementEvent extends GameEvent {
    private final UUID playerUuid;
    private final String playerName;
    private final String advancementTitle;
    private final String advancementDescription;
    private final String advancementMessage;

    public PlayerAdvancementEvent(UUID playerUuid, String playerName, String advancementTitle, String advancementDescription, String advancementMessage) {
        super(GameEventType.PLAYER_ADVANCEMENT);
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.advancementTitle = advancementTitle;
        this.advancementDescription = advancementDescription != null ? advancementDescription : "";
        this.advancementMessage = advancementMessage != null ? advancementMessage : "";
    }

    @Override
    public Map<String, String> getPlaceholders() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player_name", playerName);
        placeholders.put("player_uuid", playerUuid != null ? playerUuid.toString() : "");
        placeholders.put("advancement_title", advancementTitle);
        placeholders.put("advancement_description", advancementDescription);
        placeholders.put("advancement_message", advancementMessage);
        return placeholders;
    }
}
