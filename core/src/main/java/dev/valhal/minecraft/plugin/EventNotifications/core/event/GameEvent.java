package dev.valhal.minecraft.plugin.EventNotifications.core.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public sealed abstract class GameEvent permits
        ServerStartupEvent,
        ServerShutdownEvent,
        PlayerConnectEvent,
        PlayerDisconnectEvent,
        PlayerDeathEvent,
        PlayerBannedEvent,
        PlayerKickedEvent,
        PlayerOpEvent,
        PlayerDeopEvent,
        PlayerWhitelistedEvent,
        PlayerUnwhitelistedEvent,
        PlayerPardonedEvent,
        WhitelistOnEvent,
        WhitelistOffEvent,
        PlayerAdvancementEvent {

    private final UUID eventId;
    private final Instant timestamp;
    private final GameEventType type;

    protected GameEvent(GameEventType type) {
        this.eventId = UUID.randomUUID();
        this.timestamp = Instant.now();
        this.type = type;
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public GameEventType getType() {
        return type;
    }

    public abstract Map<String, String> getPlaceholders();
}
