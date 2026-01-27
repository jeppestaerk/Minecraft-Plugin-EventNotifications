package dev.valhal.minecraft.plugin.EventNotifications.core.notification;

import dev.valhal.minecraft.plugin.EventNotifications.core.config.AuthConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class DiscordWebhookTarget implements NotificationTarget {
    private final String name;
    private final boolean enabled;
    private final String webhookUrl;
    private final boolean useEmbeds;
    private final String icon;
    private final AuthConfig auth;
    private final HttpClient httpClient;

    public DiscordWebhookTarget(String name, boolean enabled, String webhookUrl, boolean useEmbeds, String icon, AuthConfig auth) {
        this.name = name;
        this.enabled = enabled;
        this.webhookUrl = webhookUrl;
        this.useEmbeds = useEmbeds;
        this.icon = icon;
        this.auth = auth != null ? auth : AuthConfig.none();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return "discord";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public CompletableFuture<NotificationResult> send(NotificationPayload payload) {
        if (!enabled) {
            return CompletableFuture.completedFuture(
                    NotificationResult.failure(name, "Target is disabled")
            );
        }

        String jsonBody = buildJsonBody(payload);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json");

        // Add authentication header if configured
        if (auth.hasAuth()) {
            requestBuilder.header("Authorization", auth.getAuthorizationHeader());
        }

        HttpRequest request = requestBuilder
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return NotificationResult.success(name);
                    } else {
                        return NotificationResult.failure(name,
                                "HTTP " + response.statusCode() + ": " + response.body());
                    }
                })
                .exceptionally(e -> NotificationResult.failure(name, e.getMessage()));
    }

    private String buildJsonBody(NotificationPayload payload) {
        StringBuilder json = new StringBuilder("{");

        // Add avatar_url if icon is configured
        if (icon != null && !icon.isBlank()) {
            json.append("\"avatar_url\":\"").append(escapeJson(icon)).append("\",");
        }

        if (useEmbeds) {
            int color = getColor(payload);
            json.append(String.format(
                    "\"embeds\":[{\"title\":\"%s\",\"description\":\"%s\",\"color\":%d}]",
                    escapeJson(payload.title()),
                    escapeJson(payload.message()),
                    color
            ));
        } else {
            String content = "**" + payload.title() + "**\n" + payload.message();
            json.append(String.format("\"content\":\"%s\"", escapeJson(content)));
        }

        json.append("}");
        return json.toString();
    }

    private int getColor(NotificationPayload payload) {
        // Check for color in extras first
        String colorStr = payload.getString("color");
        if (colorStr != null && !colorStr.isBlank()) {
            return parseColor(colorStr);
        }
        // Fall back to priority-based color
        return getColorForPriority(payload.getPriority());
    }

    private int parseColor(String colorStr) {
        try {
            // Remove # prefix if present
            if (colorStr.startsWith("#")) {
                colorStr = colorStr.substring(1);
            }
            // Parse as hex
            return Integer.parseInt(colorStr, 16);
        } catch (NumberFormatException e) {
            // Try parsing as decimal
            try {
                return Integer.parseInt(colorStr);
            } catch (NumberFormatException e2) {
                return 3447003; // Default blue
            }
        }
    }

    private int getColorForPriority(NotificationPriority priority) {
        return switch (priority) {
            case URGENT -> 15158332;  // Red
            case HIGH -> 15105570;    // Orange
            case DEFAULT -> 3447003;  // Blue
            case LOW -> 9807270;      // Grey
            case MIN -> 9807270;      // Grey
        };
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
