package dev.valhal.minecraft.plugin.EventNotifications.core.notification;

import dev.valhal.minecraft.plugin.EventNotifications.core.config.AuthConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class GenericWebhookTarget implements NotificationTarget {
    private final String name;
    private final boolean enabled;
    private final String url;
    private final String method;
    private final Map<String, String> headers;
    private final AuthConfig auth;
    private final HttpClient httpClient;

    public GenericWebhookTarget(String name, boolean enabled, String url, String method, Map<String, String> headers, AuthConfig auth) {
        this.name = name;
        this.enabled = enabled;
        this.url = url;
        this.method = method != null ? method.toUpperCase() : "POST";
        this.headers = headers != null ? headers : Map.of();
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
        return "webhook";
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

        String jsonBody = String.format(
                "{\"title\":\"%s\",\"message\":\"%s\",\"priority\":\"%s\"}",
                escapeJson(payload.title()),
                escapeJson(payload.message()),
                payload.getPriority().name().toLowerCase()
        );

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json");

        // Add authentication header if configured
        if (auth.hasAuth()) {
            requestBuilder.header("Authorization", auth.getAuthorizationHeader());
        }

        for (Map.Entry<String, String> header : headers.entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }

        HttpRequest request = switch (method) {
            case "PUT" -> requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
            default -> requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
        };

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
