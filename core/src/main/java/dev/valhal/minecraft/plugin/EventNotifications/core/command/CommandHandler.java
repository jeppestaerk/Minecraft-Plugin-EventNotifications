package dev.valhal.minecraft.plugin.EventNotifications.core.command;

import dev.valhal.minecraft.plugin.EventNotifications.core.config.ConfigManager;
import dev.valhal.minecraft.plugin.EventNotifications.core.config.PluginConfig;
import dev.valhal.minecraft.plugin.EventNotifications.core.config.TargetConfig;
import dev.valhal.minecraft.plugin.EventNotifications.core.notification.NotificationService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Handles all /eventnotify commands.
 * Platform-agnostic command logic that can be used by Fabric, NeoForge, and Paper adapters.
 */
public class CommandHandler {
    private final ConfigManager configManager;
    private final NotificationService notificationService;
    private final Consumer<String> logger;

    // Cached data for tab completion
    private List<String> cachedTargetNames;
    private PluginConfig cachedConfig;

    public CommandHandler(ConfigManager configManager, NotificationService notificationService, Consumer<String> logger) {
        this.configManager = configManager;
        this.notificationService = notificationService;
        this.logger = logger;
        refreshCache();
    }

    /**
     * Refreshes the cached target names and config for tab completion.
     */
    public void refreshCache() {
        cachedConfig = configManager.getCurrentConfig();
        if (cachedConfig != null) {
            cachedTargetNames = cachedConfig.targets().stream()
                    .map(TargetConfig::name)
                    .toList();
        } else {
            cachedTargetNames = List.of();
        }
    }

    /**
     * Get cached target names for tab completion.
     */
    public List<String> getTargetNames() {
        return cachedTargetNames;
    }

    /**
     * Get properties for a target type (for tab completion).
     */
    public List<String> getPropertiesForTarget(String targetName) {
        Optional<TargetConfig> target = configManager.getTarget(targetName);
        return target.map(TargetConfig::getValidProperties).orElse(List.of());
    }

    /**
     * Get the type of a target (for tab completion).
     */
    public Optional<String> getTargetType(String targetName) {
        return configManager.getTarget(targetName).map(TargetConfig::type);
    }

    // ========== Command Implementations ==========

    /**
     * /eventnotify reload - Reload configuration from disk.
     */
    public CommandResult reload() {
        try {
            PluginConfig config = configManager.reload();
            notificationService.reloadTargets(config);
            refreshCache();
            return CommandResult.success("Configuration reloaded. " + config.targets().size() + " targets loaded.");
        } catch (IOException e) {
            logger.accept("Failed to reload config: " + e.getMessage());
            return CommandResult.error("Failed to reload configuration: " + e.getMessage());
        }
    }

    /**
     * /eventnotify list - List all targets with status.
     */
    public CommandResult list() {
        PluginConfig config = configManager.getCurrentConfig();
        if (config == null) {
            return CommandResult.error("Configuration not loaded.");
        }

        if (config.targets().isEmpty()) {
            return CommandResult.success("No notification targets configured.");
        }

        return CommandResult.targetList(config.targets());
    }

    /**
     * /eventnotify info <name> - Show target details.
     */
    public CommandResult info(String targetName) {
        Optional<TargetConfig> target = configManager.getTarget(targetName);
        if (target.isEmpty()) {
            return CommandResult.error("Target not found: " + targetName);
        }

        return CommandResult.targetInfo(target.get());
    }

    /**
     * /eventnotify enable <name> - Enable a target.
     */
    public CommandResult enable(String targetName) {
        try {
            boolean success = configManager.setTargetEnabled(targetName, true);
            if (!success) {
                return CommandResult.error("Target not found: " + targetName);
            }

            notificationService.reloadTargets(configManager.getCurrentConfig());
            refreshCache();
            return CommandResult.success("Enabled target: " + targetName);
        } catch (IOException e) {
            logger.accept("Failed to enable target: " + e.getMessage());
            return CommandResult.error("Failed to save configuration: " + e.getMessage());
        }
    }

    /**
     * /eventnotify disable <name> - Disable a target.
     */
    public CommandResult disable(String targetName) {
        try {
            boolean success = configManager.setTargetEnabled(targetName, false);
            if (!success) {
                return CommandResult.error("Target not found: " + targetName);
            }

            notificationService.reloadTargets(configManager.getCurrentConfig());
            refreshCache();
            return CommandResult.success("Disabled target: " + targetName);
        } catch (IOException e) {
            logger.accept("Failed to disable target: " + e.getMessage());
            return CommandResult.error("Failed to save configuration: " + e.getMessage());
        }
    }

    /**
     * /eventnotify set <name> <property> <value> - Set a property.
     */
    public CommandResult set(String targetName, String property, String value) {
        Optional<TargetConfig> target = configManager.getTarget(targetName);
        if (target.isEmpty()) {
            return CommandResult.error("Target not found: " + targetName);
        }

        // Validate property is valid for this target type
        List<String> validProperties = target.get().getValidProperties();
        if (!validProperties.contains(property) && !property.equals("enabled")) {
            return CommandResult.error("Invalid property '" + property + "' for target type '" + target.get().type() + "'. Valid properties: " + String.join(", ", validProperties));
        }

        try {
            boolean success = configManager.setTargetProperty(targetName, property, value);
            if (!success) {
                return CommandResult.error("Failed to set property.");
            }

            notificationService.reloadTargets(configManager.getCurrentConfig());
            refreshCache();
            return CommandResult.success("Set " + property + "=" + value + " for target: " + targetName);
        } catch (IOException e) {
            logger.accept("Failed to set property: " + e.getMessage());
            return CommandResult.error("Failed to save configuration: " + e.getMessage());
        }
    }

    /**
     * /eventnotify message <name> <message> - Send custom message to target.
     */
    public CompletableFuture<CommandResult> message(String targetName, String message) {
        Optional<TargetConfig> target = configManager.getTarget(targetName);
        if (target.isEmpty()) {
            return CompletableFuture.completedFuture(CommandResult.error("Target not found: " + targetName));
        }

        if (!target.get().enabled()) {
            return CompletableFuture.completedFuture(CommandResult.error("Target is disabled: " + targetName));
        }

        return notificationService.sendMessage(targetName, message)
                .<CommandResult>thenApply(result -> {
                    if (result.success()) {
                        return CommandResult.messageSent(targetName, true, "Message sent successfully.");
                    } else {
                        return CommandResult.messageSent(targetName, false, result.message());
                    }
                })
                .exceptionally(e -> CommandResult.error("Failed to send message: " + e.getMessage()));
    }

    /**
     * Display help text.
     */
    public CommandResult help() {
        String helpText = """
            EventNotifications Commands:
            /eventnotifications reload - Reload configuration
            /eventnotifications list - List all targets
            /eventnotifications info <name> - Show target details
            /eventnotifications enable <name> - Enable a target
            /eventnotifications disable <name> - Disable a target
            /eventnotifications set <name> <property> <value> - Set a property
            /eventnotifications message <name> <message> - Send custom message
            Alias: configurable via command_alias in config.yml""";
        return CommandResult.success(helpText);
    }
}
