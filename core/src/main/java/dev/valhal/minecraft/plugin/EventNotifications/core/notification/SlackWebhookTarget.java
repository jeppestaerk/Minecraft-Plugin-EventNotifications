package dev.valhal.minecraft.plugin.EventNotifications.core.notification;

import dev.valhal.minecraft.plugin.EventNotifications.core.config.AuthConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class SlackWebhookTarget implements NotificationTarget {
    private final String name;
    private final boolean enabled;
    private final String webhookUrl;
    private final boolean useAttachments;
    private final String icon;
    private final AuthConfig auth;
    private final HttpClient httpClient;

    public SlackWebhookTarget(String name, boolean enabled, String webhookUrl, boolean useAttachments, String icon, AuthConfig auth) {
        this.name = name;
        this.enabled = enabled;
        this.webhookUrl = webhookUrl;
        this.useAttachments = useAttachments;
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
        return "slack";
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

        if (icon != null && !icon.isBlank()) {
            json.append("\"icon_url\":\"").append(escapeJson(icon)).append("\",");
        }

        if (useAttachments) {
            String color = getColor(payload);
            json.append(String.format(
                    "\"attachments\":[{\"color\":\"%s\",\"title\":\"%s\",\"text\":\"%s\"}]",
                    color,
                    escapeJson(payload.title()),
                    escapeJson(payload.message())
            ));
        } else {
            String text = "*" + payload.title() + "*\n" + payload.message();
            json.append(String.format("\"text\":\"%s\"", escapeJson(text)));
        }

        json.append("}");
        return json.toString();
    }

    private String getColor(NotificationPayload payload) {
        String colorStr = payload.getString("color");
        if (colorStr != null && !colorStr.isBlank()) {
            return colorStr.startsWith("#") ? colorStr : "#" + colorStr;
        }
        return getColorForPriority(payload.getPriority());
    }

    private String getColorForPriority(NotificationPriority priority) {
        return switch (priority) {
            case URGENT -> "#e74c3c";  // Red
            case HIGH -> "#e67e22";    // Orange
            case DEFAULT -> "#3498db"; // Blue
            case LOW -> "#95a5a6";     // Grey
            case MIN -> "#95a5a6";     // Grey
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
