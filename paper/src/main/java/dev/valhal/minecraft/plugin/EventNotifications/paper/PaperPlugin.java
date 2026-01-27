package dev.valhal.minecraft.plugin.EventNotifications.paper;

import dev.valhal.minecraft.plugin.EventNotifications.core.config.ConfigLoader;
import dev.valhal.minecraft.plugin.EventNotifications.core.config.PluginConfig;
import dev.valhal.minecraft.plugin.EventNotifications.core.config.TemplateLoader;
import dev.valhal.minecraft.plugin.EventNotifications.core.event.EventBus;
import dev.valhal.minecraft.plugin.EventNotifications.core.notification.NotificationService;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public class PaperPlugin extends JavaPlugin {
    private static final String LOG_PREFIX = "[EventNotifications] ";

    private EventBus eventBus;
    private NotificationService notificationService;
    private PaperAsyncExecutor asyncExecutor;
    private PaperEventAdapter eventAdapter;

    @Override
    public void onEnable() {
        try {
            // Save default config if it doesn't exist (Paper convention)
            saveDefaultConfig();

            Path configDir = getDataFolder().toPath();
            Path configFile = configDir.resolve("config.yml");
            Path templatesDir = configDir.resolve("templates");

            ConfigLoader configLoader = new ConfigLoader(configFile);
            PluginConfig config = configLoader.load();

            TemplateLoader templateLoader = new TemplateLoader(templatesDir);
            templateLoader.load();

            eventBus = new EventBus();
            asyncExecutor = new PaperAsyncExecutor();

            notificationService = new NotificationService(config, templateLoader, asyncExecutor, msg -> log(msg));
            notificationService.registerWithEventBus(eventBus);

            // Only use MOTD as server name if config doesn't specify one
            boolean useMotdAsServerName = config.serverName() == null || config.serverName().isBlank();

            eventAdapter = new PaperEventAdapter(this, eventBus, serverName -> {
                if (useMotdAsServerName) {
                    notificationService.updateServerName(serverName);
                }
            });
            eventAdapter.register();

            log("Loaded with {} notification targets", notificationService.getTargets().size());

        } catch (Exception e) {
            logError("Failed to initialize", e);
        }
    }

    @Override
    public void onDisable() {
        if (eventAdapter != null) {
            eventAdapter.onServerShutdown();
        }
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
        }
    }

    public static void log(String message, Object... args) {
        String formatted = message;
        for (Object arg : args) {
            formatted = formatted.replaceFirst("\\{}", String.valueOf(arg));
        }
        org.bukkit.Bukkit.getLogger().info(LOG_PREFIX + formatted);
    }

    public static void logError(String message, Throwable e) {
        org.bukkit.Bukkit.getLogger().severe(LOG_PREFIX + message + ": " + e.getMessage());
    }
}
