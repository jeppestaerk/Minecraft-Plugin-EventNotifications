package dev.valhal.minecraft.plugin.EventNotifications.paper;

import dev.valhal.minecraft.plugin.EventNotifications.core.event.*;
import io.papermc.paper.ban.BanListType;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.ban.ProfileBanList;
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
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

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
                // Use Adventure API - motd() returns a Component
                String motd = PLAIN_SERIALIZER.serialize(Bukkit.motd());
                if (!motd.isBlank()) {
                    serverNameCallback.accept(motd);
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
        // Use Adventure API - deathMessage() returns a Component
        var deathMessageComponent = event.deathMessage();
        String deathMessage = deathMessageComponent != null
                ? PLAIN_SERIALIZER.serialize(deathMessageComponent)
                : player.getName() + " died";
        eventBus.publish(new dev.valhal.minecraft.plugin.EventNotifications.core.event.PlayerDeathEvent(
                player.getUniqueId(),
                player.getName(),
                deathMessage
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        // Use Adventure API - reason() returns a Component
        String reason = PLAIN_SERIALIZER.serialize(event.reason());

        // Check if this is a ban kick using the profile-based ban list
        ProfileBanList banList = Bukkit.getBanList(BanListType.PROFILE);
        if (banList.isBanned(player.getPlayerProfile())) {
            var banEntry = banList.getBanEntry(player.getPlayerProfile());
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
        String title = PLAIN_SERIALIZER.serialize(advancement.getDisplay().title());
        String description = PLAIN_SERIALIZER.serialize(advancement.getDisplay().description());

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
}
