package dev.valhal.minecraft.plugin.EventNotifications.core.notification;

import dev.valhal.minecraft.plugin.EventNotifications.core.config.AuthConfig;
import dev.valhal.minecraft.plugin.EventNotifications.core.config.PluginConfig;
import dev.valhal.minecraft.plugin.EventNotifications.core.config.TargetConfig;
import dev.valhal.minecraft.plugin.EventNotifications.core.config.TemplateConfig;
import dev.valhal.minecraft.plugin.EventNotifications.core.config.TemplateLoader;
import dev.valhal.minecraft.plugin.EventNotifications.core.event.EventBus;
import dev.valhal.minecraft.plugin.EventNotifications.core.event.GameEvent;
import dev.valhal.minecraft.plugin.EventNotifications.core.template.TemplateEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class NotificationService {
    private PluginConfig config;
    private final TemplateLoader templateLoader;
    private final TemplateEngine templateEngine;
    private final List<NotificationTarget> targets;
    private final RateLimiter rateLimiter;
    private final Executor executor;
    private final Consumer<String> logger;

    public NotificationService(PluginConfig config, TemplateLoader templateLoader, Executor executor, Consumer<String> logger) {
        this.config = config;
        this.templateLoader = templateLoader;
        this.executor = executor;
        this.logger = logger;
        this.templateEngine = new TemplateEngine(config.serverName());
        this.targets = new ArrayList<>();
        this.rateLimiter = RateLimiter.perMinute(30);

        initializeTargets(config);
    }

    private void initializeTargets(PluginConfig pluginConfig) {
        for (TargetConfig targetConfig : pluginConfig.targets()) {
            NotificationTarget target = createTarget(targetConfig);
            if (target != null) {
                targets.add(target);
            }
        }
    }

    /**
     * Reloads targets from a new configuration.
     * Clears existing targets and reinitializes from the provided config.
     */
    public void reloadTargets(PluginConfig newConfig) {
        this.config = newConfig;
        targets.clear();
        initializeTargets(newConfig);

        // Update server name in template engine if changed
        if (newConfig.serverName() != null && !newConfig.serverName().isBlank()) {
            templateEngine.setGlobalPlaceholder("server_name", newConfig.serverName());
        }

        logger.accept("Reloaded " + targets.size() + " notification targets");
    }

    private NotificationTarget createTarget(TargetConfig config) {
        AuthConfig auth = AuthConfig.fromConfig(config);
        String icon = config.getString("icon");

        return switch (config.type().toLowerCase()) {
            case "ntfy" -> new NtfyTarget(
                    config.name(),
                    config.enabled(),
                    config.getString("server"),
                    config.getString("topic"),
                    config.getBoolean("markdown", true),
                    icon,
                    auth
            );
            case "discord" -> new DiscordWebhookTarget(
                    config.name(),
                    config.enabled(),
                    config.getString("webhook_url"),
                    config.getBoolean("use_embeds", true),
                    icon,
                    auth
            );
            case "slack" -> new SlackWebhookTarget(
                    config.name(),
                    config.enabled(),
                    config.getString("webhook_url"),
                    config.getBoolean("use_attachments", true),
                    icon,
                    auth
            );
            case "webhook" -> new GenericWebhookTarget(
                    config.name(),
                    config.enabled(),
                    config.getString("url"),
                    config.getString("method", "POST"),
                    null,
                    auth
            );
            default -> {
                logger.accept("Unknown target type: " + config.type());
                yield null;
            }
        };
    }

    public void registerWithEventBus(EventBus eventBus) {
        eventBus.subscribe(this::handleEvent);
    }

    private void handleEvent(GameEvent event) {
        if (!rateLimiter.tryAcquire()) {
            return;
        }

        for (NotificationTarget target : targets) {
            if (!target.isEnabled()) {
                continue;
            }

            // Get template specific to this target type
            TemplateConfig.EventTemplate template = templateLoader.getTemplate(event.getType(), target.getType());

            if (!template.enabled()) {
                continue;
            }

            String title = templateEngine.render(template.title(), event.getPlaceholders());
            String message = templateEngine.render(template.message(), event.getPlaceholders());

            // Render any string extras that might contain placeholders (e.g., tags with {{player_name}})
            Map<String, Object> renderedExtras = new HashMap<>();
            for (Map.Entry<String, Object> entry : template.extras().entrySet()) {
                if (entry.getValue() instanceof String strValue) {
                    renderedExtras.put(entry.getKey(), templateEngine.render(strValue, event.getPlaceholders()));
                } else {
                    renderedExtras.put(entry.getKey(), entry.getValue());
                }
            }

            // Add priority to extras (used by ntfy)
            renderedExtras.put("priority", template.priority().name().toLowerCase());

            NotificationPayload payload = new NotificationPayload(title, message, renderedExtras);

            CompletableFuture.runAsync(() -> {
                target.send(payload).thenAccept(result -> {
                    if (!result.success()) {
                        logger.accept("Failed to send notification to " + result.targetName() + ": " + result.message());
                    }
                });
            }, executor);
        }
    }

    public List<NotificationTarget> getTargets() {
        return List.copyOf(targets);
    }

    public void updateServerName(String serverName) {
        templateEngine.setGlobalPlaceholder("server_name", serverName);
    }

    /**
     * Sends a custom message to a specific target.
     *
     * @param targetName The name of the target to send to
     * @param message    The message to send
     * @return A CompletableFuture with the notification result
     */
    public CompletableFuture<NotificationResult> sendMessage(String targetName, String message) {
        Optional<NotificationTarget> targetOpt = targets.stream()
                .filter(t -> t.getName().equalsIgnoreCase(targetName))
                .findFirst();

        if (targetOpt.isEmpty()) {
            return CompletableFuture.completedFuture(
                    NotificationResult.failure(targetName, "Target not found")
            );
        }

        NotificationTarget target = targetOpt.get();

        if (!target.isEnabled()) {
            return CompletableFuture.completedFuture(
                    NotificationResult.failure(targetName, "Target is disabled")
            );
        }

        // Use message template
        TemplateConfig.EventTemplate template = templateLoader.getMessageTemplate();

        // Create placeholders map with the message content
        Map<String, String> placeholders = Map.of("message", message);

        // Render title and message using template
        String title = templateEngine.render(template.title(), placeholders);
        String renderedMessage = templateEngine.render(template.message(), placeholders);

        // Build extras from template
        Map<String, Object> extras = new HashMap<>(template.extras());

        NotificationPayload payload = new NotificationPayload(title, renderedMessage, extras);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return target.send(payload).join();
            } catch (Exception e) {
                return NotificationResult.failure(targetName, e.getMessage());
            }
        }, executor);
    }

    /**
     * Gets a target by name.
     */
    public Optional<NotificationTarget> getTarget(String name) {
        return targets.stream()
                .filter(t -> t.getName().equalsIgnoreCase(name))
                .findFirst();
    }
}
