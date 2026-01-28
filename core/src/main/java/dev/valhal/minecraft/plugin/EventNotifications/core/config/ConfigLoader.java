package dev.valhal.minecraft.plugin.EventNotifications.core.config;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
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

    public Path getConfigPath() {
        return configPath;
    }

    public PluginConfig load() throws IOException {
        ensureConfigExists();

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configPath)
                .nodeStyle(NodeStyle.BLOCK)
                .build();

        ConfigurationNode root = loader.load();

        // Merge defaults for any missing config options
        mergeDefaults(root, loader);

        // Load general settings
        GeneralConfig general = loadGeneralConfig(root.node("general"));
        List<TargetConfig> targets = loadTargets(root.node("targets"));

        return new PluginConfig(general, targets);
    }

    private GeneralConfig loadGeneralConfig(ConfigurationNode node) {
        String serverName = node.node("server_name").getString("");
        boolean commandsEnabled = node.node("commands_enabled").getBoolean(true);
        String commandAlias = node.node("command_alias").getString("en");
        return new GeneralConfig(serverName, commandsEnabled, commandAlias);
    }

    /**
     * Merges default values into the config file for any missing options.
     * Similar to TemplateLoader.mergeDefaults() - ensures user configs get updated
     * with new options added in newer versions.
     */
    private void mergeDefaults(ConfigurationNode root, YamlConfigurationLoader loader) throws IOException {
        boolean updated = false;

        ConfigurationNode generalNode = root.node("general");

        // Default values for general options (use LinkedHashMap to preserve order)
        Map<String, Object> generalDefaults = new LinkedHashMap<>();
        generalDefaults.put("server_name", "");
        generalDefaults.put("commands_enabled", true);
        generalDefaults.put("command_alias", "notify");

        for (Map.Entry<String, Object> entry : generalDefaults.entrySet()) {
            ConfigurationNode node = generalNode.node(entry.getKey());
            if (node.virtual()) {
                node.set(entry.getValue());
                updated = true;
            }
        }

        if (updated) {
            // Rebuild root with correct order: general first, then targets
            ConfigurationNode newRoot = loader.createNode();
            newRoot.node("general").set(root.node("general"));
            newRoot.node("targets").set(root.node("targets"));
            loader.save(newRoot);
        }
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
                  # Enable in-game commands (/eventnotifications) for managing targets
                  commands_enabled: true
                  # Command alias (e.g., "en" creates /en as shortcut, empty string to disable)
                  command_alias: "en"

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

    public void save(PluginConfig config) throws IOException {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configPath)
                .nodeStyle(NodeStyle.BLOCK)
                .build();

        ConfigurationNode root = loader.createNode();

        // Save general section
        saveGeneralConfig(root.node("general"), config.general());

        // Save targets
        ConfigurationNode targetsNode = root.node("targets");
        for (TargetConfig target : config.targets()) {
            ConfigurationNode targetNode = targetsNode.node(target.name());
            targetNode.node("type").set(target.type());
            targetNode.node("enabled").set(target.enabled());

            for (Map.Entry<String, Object> prop : target.properties().entrySet()) {
                targetNode.node(prop.getKey()).set(prop.getValue());
            }
        }

        loader.save(root);
    }

    private void saveGeneralConfig(ConfigurationNode node, GeneralConfig general) throws IOException {
        node.node("server_name").set(general.serverName());
        node.node("commands_enabled").set(general.commandsEnabled());
        node.node("command_alias").set(general.commandAlias());
    }
}
