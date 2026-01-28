package dev.valhal.minecraft.plugin.EventNotifications.neoforge;

import dev.valhal.minecraft.plugin.EventNotifications.core.command.CommandHandler;
import dev.valhal.minecraft.plugin.EventNotifications.core.config.ConfigLoader;
import dev.valhal.minecraft.plugin.EventNotifications.core.config.ConfigManager;
import dev.valhal.minecraft.plugin.EventNotifications.core.config.PluginConfig;
import dev.valhal.minecraft.plugin.EventNotifications.core.config.TemplateLoader;
import dev.valhal.minecraft.plugin.EventNotifications.core.event.EventBus;
import dev.valhal.minecraft.plugin.EventNotifications.core.notification.NotificationService;
import dev.valhal.minecraft.plugin.EventNotifications.neoforge.command.NeoForgeCommandAdapter;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

@Mod(NeoForgeMod.MOD_ID)
public class NeoForgeMod {
    public static final String MOD_ID = "eventnotifications";
    private static final Logger LOGGER = LogManager.getLogger();

    private EventBus eventBus;
    private NotificationService notificationService;
    private NeoForgeAsyncExecutor asyncExecutor;
    private ConfigManager configManager;
    private CommandHandler commandHandler;

    public NeoForgeMod(IEventBus modEventBus) {
        try {
            Path configDir = FMLPaths.CONFIGDIR.get().resolve(MOD_ID);
            Path configFile = configDir.resolve("config.yml");
            Path templatesDir = configDir.resolve("templates");

            ConfigLoader configLoader = new ConfigLoader(configFile);
            configManager = new ConfigManager(configLoader, msg -> log(msg));
            PluginConfig config = configManager.load();

            TemplateLoader templateLoader = new TemplateLoader(templatesDir);
            templateLoader.load();

            eventBus = new EventBus();
            asyncExecutor = new NeoForgeAsyncExecutor();

            notificationService = new NotificationService(config, templateLoader, asyncExecutor, msg -> log(msg));
            notificationService.registerWithEventBus(eventBus);

            // Register commands if enabled in config
            if (config.commandsEnabled()) {
                commandHandler = new CommandHandler(configManager, notificationService, msg -> log(msg));
                NeoForgeCommandAdapter commandAdapter = new NeoForgeCommandAdapter(commandHandler, config.commandAlias());
                NeoForge.EVENT_BUS.addListener(commandAdapter::onRegisterCommands);
                log("Commands enabled");
            } else {
                log("Commands disabled in config");
            }

            // Only use MOTD as server name if config doesn't specify one
            boolean useMotdAsServerName = config.serverName() == null || config.serverName().isBlank();

            NeoForgeEventAdapter eventAdapter = new NeoForgeEventAdapter(eventBus, serverName -> {
                if (useMotdAsServerName) {
                    notificationService.updateServerName(serverName);
                }
            });

            NeoForge.EVENT_BUS.register(eventAdapter);

            log("Loaded with {} notification targets", notificationService.getTargets().size());

        } catch (Exception e) {
            logError("Failed to initialize", e);
        }
    }

    public static void log(String message, Object... args) {
        String formatted = message;
        for (Object arg : args) {
            formatted = formatted.replaceFirst("\\{}", String.valueOf(arg));
        }
        LOGGER.info("[EventNotifications] {}", formatted);
    }

    public static void logError(String message, Throwable e) {
        LOGGER.error("[EventNotifications] {}", message, e);
    }
}
