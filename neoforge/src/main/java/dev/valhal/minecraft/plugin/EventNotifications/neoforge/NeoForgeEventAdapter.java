package dev.valhal.minecraft.plugin.EventNotifications.neoforge;

import dev.valhal.minecraft.plugin.EventNotifications.core.event.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.function.Consumer;

public class NeoForgeEventAdapter {
    private final EventBus eventBus;
    private final Consumer<String> serverNameCallback;

    public NeoForgeEventAdapter(EventBus eventBus, Consumer<String> serverNameCallback) {
        this.eventBus = eventBus;
        this.serverNameCallback = serverNameCallback;
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        String motd = event.getServer().getMotd();
        if (motd != null && !motd.isBlank()) {
            serverNameCallback.accept(motd);
        } else {
            serverNameCallback.accept("Minecraft Server");
        }
        eventBus.publish(new ServerStartupEvent());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        eventBus.publish(new ServerShutdownEvent());
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            eventBus.publish(new PlayerConnectEvent(
                    player.getUUID(),
                    player.getName().getString()
            ));
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            eventBus.publish(new PlayerDisconnectEvent(
                    player.getUUID(),
                    player.getName().getString()
            ));
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String deathMessage = player.getCombatTracker().getDeathMessage().getString();
            eventBus.publish(new PlayerDeathEvent(
                    player.getUUID(),
                    player.getName().getString(),
                    deathMessage
            ));
        }
    }

    @SubscribeEvent
    public void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var advancement = event.getAdvancement();
            var display = advancement.value().display().orElse(null);

            // Skip non-display advancements (recipes, etc.)
            if (display == null) {
                return;
            }

            String title = display.getTitle().getString();
            String description = display.getDescription().getString();
            String message = player.getName().getString() + " has made the advancement [" + title + "]";

            eventBus.publish(new PlayerAdvancementEvent(
                    player.getUUID(),
                    player.getName().getString(),
                    title,
                    description,
                    message
            ));
        }
    }
}
