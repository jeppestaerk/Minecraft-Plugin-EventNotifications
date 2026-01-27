package dev.valhal.minecraft.plugin.EventNotifications.fabric;

import dev.valhal.minecraft.plugin.EventNotifications.core.event.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.function.Consumer;

public class FabricEventAdapter {
    private final EventBus eventBus;
    private final Consumer<String> serverNameCallback;
    private static FabricEventAdapter instance;

    public FabricEventAdapter(EventBus eventBus, Consumer<String> serverNameCallback) {
        this.eventBus = eventBus;
        this.serverNameCallback = serverNameCallback;
        instance = this;
    }

    public static FabricEventAdapter getInstance() {
        return instance;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public void register() {
        // Server lifecycle events
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Provide the server MOTD as the server name if config didn't specify one
            String motd = server.getServerMotd();
            if (motd != null && !motd.isBlank()) {
                serverNameCallback.accept(motd);
            } else {
                // Fallback if MOTD is also empty
                serverNameCallback.accept("Minecraft Server");
            }
            eventBus.publish(new ServerStartupEvent());
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            eventBus.publish(new ServerShutdownEvent());
        });

        // Player connection events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.getPlayer();
            eventBus.publish(new PlayerConnectEvent(
                    player.getUuid(),
                    player.getName().getString()
            ));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            var player = handler.getPlayer();
            eventBus.publish(new PlayerDisconnectEvent(
                    player.getUuid(),
                    player.getName().getString()
            ));
        });

        // Player death event - use ALLOW_DEATH to capture message before tracker is cleared
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                // Use DamageTracker to get the same message that's sent to chat
                String deathMessage = player.getDamageTracker().getDeathMessage().getString();
                eventBus.publish(new PlayerDeathEvent(
                        player.getUuid(),
                        player.getName().getString(),
                        deathMessage
                ));
            }
            return true; // Allow death to proceed
        });
    }

    // Methods to be called from mixins
    public void onPlayerBanned(String playerName, java.util.UUID playerUuid, String reason, String bannedBy) {
        eventBus.publish(new PlayerBannedEvent(playerUuid, playerName, reason, bannedBy));
    }

    public void onPlayerKicked(java.util.UUID playerUuid, String playerName, String reason) {
        eventBus.publish(new PlayerKickedEvent(playerUuid, playerName, reason));
    }

    public void onPlayerOp(java.util.UUID playerUuid, String playerName) {
        eventBus.publish(new PlayerOpEvent(playerUuid, playerName));
    }

    public void onPlayerDeop(java.util.UUID playerUuid, String playerName) {
        eventBus.publish(new PlayerDeopEvent(playerUuid, playerName));
    }

    public void onPlayerWhitelisted(java.util.UUID playerUuid, String playerName) {
        eventBus.publish(new PlayerWhitelistedEvent(playerUuid, playerName));
    }

    public void onPlayerUnwhitelisted(java.util.UUID playerUuid, String playerName) {
        eventBus.publish(new PlayerUnwhitelistedEvent(playerUuid, playerName));
    }

    public void onPlayerPardoned(java.util.UUID playerUuid, String playerName) {
        eventBus.publish(new PlayerPardonedEvent(playerUuid, playerName));
    }

    public void onWhitelistOn() {
        eventBus.publish(new WhitelistOnEvent());
    }

    public void onWhitelistOff() {
        eventBus.publish(new WhitelistOffEvent());
    }

    public void onPlayerAdvancement(java.util.UUID playerUuid, String playerName, String advancementTitle, String advancementDescription, String advancementMessage) {
        eventBus.publish(new PlayerAdvancementEvent(playerUuid, playerName, advancementTitle, advancementDescription, advancementMessage));
    }
}
