package dev.valhal.minecraft.plugin.EventNotifications.fabric;

import dev.valhal.minecraft.plugin.EventNotifications.core.command.CommandHandler;
import dev.valhal.minecraft.plugin.EventNotifications.core.config.ConfigLoader;
import dev.valhal.minecraft.plugin.EventNotifications.core.config.ConfigManager;
import dev.valhal.minecraft.plugin.EventNotifications.core.config.PluginConfig;
import dev.valhal.minecraft.plugin.EventNotifications.core.config.TemplateLoader;
import dev.valhal.minecraft.plugin.EventNotifications.core.event.EventBus;
import dev.valhal.minecraft.plugin.EventNotifications.core.notification.NotificationService;
import dev.valhal.minecraft.plugin.EventNotifications.fabric.command.FabricCommandAdapter;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class FabricMod implements ModInitializer, DedicatedServerModInitializer {
    public static final String MOD_ID = "eventnotifications";
    private static final String LOG_PREFIX = "[EventNotifications] ";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void log(String message, Object... args) {
        LOGGER.info(LOG_PREFIX + message, args);
    }

    public static void logError(String message, Throwable e) {
        LOGGER.error(LOG_PREFIX + message, e);
    }

    private static boolean initialized = false;
    private EventBus eventBus;
    private NotificationService notificationService;
    private FabricAsyncExecutor asyncExecutor;
    private ConfigManager configManager;
    private CommandHandler commandHandler;

    @Override
    public void onInitialize() {
        initialize();
    }

    @Override
    public void onInitializeServer() {
        initialize();
    }

    private synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        try {
            Path configDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
            Path configFile = configDir.resolve("config.yml");
            Path templatesDir = configDir.resolve("templates");

            ConfigLoader configLoader = new ConfigLoader(configFile);
            configManager = new ConfigManager(configLoader, msg -> log(msg));
            PluginConfig config = configManager.load();

            TemplateLoader templateLoader = new TemplateLoader(templatesDir);
            templateLoader.load();

            eventBus = new EventBus();
            asyncExecutor = new FabricAsyncExecutor();

            notificationService = new NotificationService(config, templateLoader, asyncExecutor, msg -> log(msg));
            notificationService.registerWithEventBus(eventBus);

            // Register commands if enabled in config
            if (config.commandsEnabled()) {
                commandHandler = new CommandHandler(configManager, notificationService, msg -> log(msg));
                FabricCommandAdapter commandAdapter = new FabricCommandAdapter(commandHandler, config.commandAlias());
                commandAdapter.register();
                log("Commands enabled");
            } else {
                log("Commands disabled in config");
            }

            // Only use MOTD as server name if config doesn't specify one
            boolean useMotdAsServerName = config.serverName() == null || config.serverName().isBlank();

            FabricEventAdapter eventAdapter = new FabricEventAdapter(eventBus, serverName -> {
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
}
