package dev.valhal.minecraft.plugin.EventNotifications.core.config;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ConfigLoader {
    private final Path configPath;

    public ConfigLoader(Path configPath) {
        this.configPath = configPath;
    }

    public PluginConfig load() throws IOException {
        ensureConfigExists();

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configPath)
                .build();

        ConfigurationNode root = loader.load();

        // Server name from config, or empty to use MOTD at runtime
        String serverName = root.node("general", "server_name").getString("");
        List<TargetConfig> targets = loadTargets(root.node("targets"));

        return new PluginConfig(serverName, targets);
    }

    private void ensureConfigExists() throws IOException {
        if (Files.exists(configPath)) {
            return;
        }

        Files.createDirectories(configPath.getParent());

        try (InputStream defaultConfig = getClass().getResourceAsStream("/config.yml")) {
            if (defaultConfig != null) {
                Files.copy(defaultConfig, configPath);
            } else {
                Files.writeString(configPath, getDefaultConfigContent());
            }
        }
    }

    private List<TargetConfig> loadTargets(ConfigurationNode targetsNode) {
        List<TargetConfig> targets = new ArrayList<>();

        for (Map.Entry<Object, ? extends ConfigurationNode> entry : targetsNode.childrenMap().entrySet()) {
            String name = entry.getKey().toString();
            ConfigurationNode targetNode = entry.getValue();

            String type = targetNode.node("type").getString("webhook");
            boolean enabled = targetNode.node("enabled").getBoolean(true);

            Map<String, Object> properties = new HashMap<>();
            for (Map.Entry<Object, ? extends ConfigurationNode> prop : targetNode.childrenMap().entrySet()) {
                String key = prop.getKey().toString();
                if (!key.equals("type") && !key.equals("enabled")) {
                    properties.put(key, prop.getValue().raw());
                }
            }

            targets.add(new TargetConfig(name, type, enabled, properties));
        }

        return targets;
    }

    private String getDefaultConfigContent() {
        return """
                # Valhal Event Notifications Configuration
                # Event templates are in the templates/ folder

                general:
                  # Server name for notifications (optional - uses server MOTD if not set)
                  server_name: ""

                # Notification targets
                # Uncomment and configure the targets you want to use

                targets:
                  # ntfy - Push notifications via ntfy.sh or self-hosted
                  # ntfy_main:
                  #   type: ntfy
                  #   enabled: true
                  #   server: "https://ntfy.sh"
                  #   topic: "my-secret-topic"
                  #   markdown: true  # Enable markdown in messages (default: true)

                  # Discord - Webhook notifications
                  # discord_main:
                  #   type: discord
                  #   enabled: true
                  #   webhook_url: "https://discord.com/api/webhooks/..."
                  #   use_embeds: true

                  # Generic webhook
                  # webhook_custom:
                  #   type: webhook
                  #   enabled: true
                  #   url: "https://example.com/webhook"
                  #   method: "POST"
                """;
    }
}
