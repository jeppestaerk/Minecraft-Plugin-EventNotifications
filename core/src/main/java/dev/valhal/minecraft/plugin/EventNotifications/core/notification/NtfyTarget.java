package dev.valhal.minecraft.plugin.EventNotifications.core.notification;

import dev.valhal.minecraft.plugin.EventNotifications.core.config.AuthConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class NtfyTarget implements NotificationTarget {
    private static final String DEFAULT_SERVER = "https://ntfy.sh";

    private final String name;
    private final boolean enabled;
    private final String server;
    private final String topic;
    private final boolean markdown;
    private final String icon;
    private final AuthConfig auth;
    private final HttpClient httpClient;

    public NtfyTarget(String name, boolean enabled, String server, String topic, boolean markdown, String icon, AuthConfig auth) {
        this.name = name;
        this.enabled = enabled;
        this.server = server != null && !server.isBlank() ? server : DEFAULT_SERVER;
        this.topic = topic;
        this.markdown = markdown;
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
        return "ntfy";
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

        String url = server + "/" + topic;
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Title", payload.title())
                .header("Priority", String.valueOf(payload.getPriority().getLevel()));

        // Add authentication header if configured
        if (auth.hasAuth()) {
            requestBuilder.header("Authorization", auth.getAuthorizationHeader());
        }

        // Add optional ntfy-specific headers from extras
        String tags = payload.getString("tags");
        if (tags != null && !tags.isBlank()) {
            requestBuilder.header("Tags", tags);
        }

        // Markdown is configured at target level
        if (markdown) {
            requestBuilder.header("Markdown", "yes");
        }

        // Icon is configured at target level
        if (icon != null && !icon.isBlank()) {
            requestBuilder.header("Icon", icon);
        }

        HttpRequest request = requestBuilder
                .POST(HttpRequest.BodyPublishers.ofString(payload.message()))
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
}
