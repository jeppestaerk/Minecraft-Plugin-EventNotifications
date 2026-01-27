package dev.valhal.minecraft.plugin.EventNotifications.paper;

import dev.valhal.minecraft.plugin.EventNotifications.core.event.*;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

public class PaperEventAdapter implements Listener {
    private final JavaPlugin plugin;
    private final EventBus eventBus;
    private final Consumer<String> serverNameCallback;
    private boolean startupFired = false;

    public PaperEventAdapter(JavaPlugin plugin, EventBus eventBus, Consumer<String> serverNameCallback) {
        this.plugin = plugin;
        this.eventBus = eventBus;
        this.serverNameCallback = serverNameCallback;
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Fire startup event and get server name
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!startupFired) {
                startupFired = true;
                String motd = Bukkit.getMotd();
                if (motd != null && !motd.isBlank()) {
                    // Strip color codes from MOTD
                    serverNameCallback.accept(stripColors(motd));
                } else {
                    serverNameCallback.accept("Minecraft Server");
                }
                eventBus.publish(new ServerStartupEvent());
            }
        });
    }

    public void onServerShutdown() {
        eventBus.publish(new ServerShutdownEvent());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        eventBus.publish(new PlayerConnectEvent(
                player.getUniqueId(),
                player.getName()
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        eventBus.publish(new PlayerDisconnectEvent(
                player.getUniqueId(),
                player.getName()
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String deathMessage = event.getDeathMessage();
        if (deathMessage == null) {
            deathMessage = player.getName() + " died";
        }
        eventBus.publish(new dev.valhal.minecraft.plugin.EventNotifications.core.event.PlayerDeathEvent(
                player.getUniqueId(),
                player.getName(),
                deathMessage
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        String reason = event.getReason();

        // Check if this is a ban kick
        if (Bukkit.getBanList(BanList.Type.NAME).isBanned(player.getName())) {
            var banEntry = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(player.getName());
            String banReason = banEntry != null && banEntry.getReason() != null ? banEntry.getReason() : "Banned";
            String bannedBy = banEntry != null && banEntry.getSource() != null ? banEntry.getSource() : "Server";
            eventBus.publish(new PlayerBannedEvent(
                    player.getUniqueId(),
                    player.getName(),
                    banReason,
                    bannedBy
            ));
        } else {
            eventBus.publish(new PlayerKickedEvent(
                    player.getUniqueId(),
                    player.getName(),
                    reason
            ));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAdvancement(PlayerAdvancementDoneEvent event) {
        Advancement advancement = event.getAdvancement();
        Player player = event.getPlayer();

        // Skip recipe unlocks and other non-display advancements
        if (advancement.getDisplay() == null) {
            return;
        }

        // Paper uses Adventure components - convert to plain text
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(advancement.getDisplay().title());
        String description = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(advancement.getDisplay().description());

        // Build announcement message similar to vanilla
        String message = player.getName() + " has made the advancement [" + title + "]";

        eventBus.publish(new PlayerAdvancementEvent(
                player.getUniqueId(),
                player.getName(),
                title,
                description,
                message
        ));
    }

    private String stripColors(String text) {
        if (text == null) return null;
        // Strip both legacy color codes (&x, section sign) and Adventure format
        return text.replaceAll("(?i)[&\u00A7][0-9A-FK-OR]", "")
                   .replaceAll("<[^>]+>", "");
    }
}
