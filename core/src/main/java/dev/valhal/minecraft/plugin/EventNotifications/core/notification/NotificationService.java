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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class NotificationService {
    private final PluginConfig config;
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

        initializeTargets();
    }

    private void initializeTargets() {
        for (TargetConfig targetConfig : config.targets()) {
            NotificationTarget target = createTarget(targetConfig);
            if (target != null) {
                targets.add(target);
            }
        }
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
}
