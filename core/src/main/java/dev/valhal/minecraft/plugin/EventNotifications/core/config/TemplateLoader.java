package dev.valhal.minecraft.plugin.EventNotifications.core.config;

import dev.valhal.minecraft.plugin.EventNotifications.core.event.GameEventType;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TemplateLoader {
    private final Path templatesDir;
    private final Map<String, TemplateConfig> templatesByType;
    private TemplateConfig defaultTemplates;

    public TemplateLoader(Path templatesDir) {
        this.templatesDir = templatesDir;
        this.templatesByType = new HashMap<>();
    }

    public void load() throws IOException {
        ensureTemplatesExist();

        // Load default templates (required)
        defaultTemplates = loadTemplateFile(templatesDir.resolve("default.yml"));

        // Load target-specific templates (optional)
        loadOptionalTemplate("ntfy");
        loadOptionalTemplate("discord");
        loadOptionalTemplate("webhook");
    }

    private void loadOptionalTemplate(String targetType) {
        Path templatePath = templatesDir.resolve(targetType + ".yml");
        if (Files.exists(templatePath)) {
            try {
                templatesByType.put(targetType, loadTemplateFile(templatePath));
            } catch (IOException e) {
                // Skip if can't load, will fall back to default
            }
        }
    }

    private void ensureTemplatesExist() throws IOException {
        Files.createDirectories(templatesDir);

        // Create default.yml if it doesn't exist
        Path defaultPath = templatesDir.resolve("default.yml");
        if (!Files.exists(defaultPath)) {
            try (InputStream defaultTemplate = getClass().getResourceAsStream("/templates/default.yml")) {
                if (defaultTemplate != null) {
                    Files.copy(defaultTemplate, defaultPath);
                } else {
                    Files.writeString(defaultPath, getDefaultTemplateContent());
                }
            }
        }
    }

    private TemplateConfig loadTemplateFile(Path path) throws IOException {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(path)
                .build();

        ConfigurationNode root = loader.load();
        Map<GameEventType, TemplateConfig.EventTemplate> templates = new EnumMap<>(GameEventType.class);

        for (GameEventType type : GameEventType.values()) {
            ConfigurationNode eventNode = root.node(type.getConfigKey());
            if (!eventNode.virtual()) {
                templates.put(type, loadEventTemplate(eventNode));
            }
        }

        // Merge defaults for any missing templates
        mergeDefaults(root, path, loader);

        return new TemplateConfig(templates);
    }

    private void mergeDefaults(ConfigurationNode root, Path path, YamlConfigurationLoader loader) throws IOException {
        boolean updated = false;

        for (GameEventType type : GameEventType.values()) {
            ConfigurationNode eventNode = root.node(type.getConfigKey());
            if (eventNode.virtual()) {
                TemplateConfig.EventTemplate defaultTemplate = getBuiltInDefault(type);
                eventNode.node("enabled").set(defaultTemplate.enabled());
                eventNode.node("title").set(defaultTemplate.title());
                eventNode.node("message").set(defaultTemplate.message());
                // Write extras (priority, tags, color, etc.)
                for (Map.Entry<String, Object> extra : defaultTemplate.extras().entrySet()) {
                    eventNode.node(extra.getKey()).set(extra.getValue());
                }
                updated = true;
            }
        }

        if (updated) {
            loader.save(root);
        }
    }

    private TemplateConfig.EventTemplate loadEventTemplate(ConfigurationNode node) {
        boolean enabled = node.node("enabled").getBoolean(true);
        String title = node.node("title").getString("");
        String message = node.node("message").getString("");

        // Load extra properties (priority, tags, color, etc.)
        Map<String, Object> extras = new HashMap<>();
        Set<String> standardKeys = Set.of("enabled", "title", "message");

        for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
            String key = entry.getKey().toString();
            if (!standardKeys.contains(key)) {
                extras.put(key, entry.getValue().raw());
            }
        }

        return new TemplateConfig.EventTemplate(enabled, title, message, extras);
    }

    public TemplateConfig.EventTemplate getTemplate(GameEventType type, String targetType) {
        // Check target-specific template first
        TemplateConfig targetTemplates = templatesByType.get(targetType.toLowerCase());
        if (targetTemplates != null && targetTemplates.hasTemplate(type)) {
            return targetTemplates.getTemplate(type);
        }

        // Fall back to default
        if (defaultTemplates != null && defaultTemplates.hasTemplate(type)) {
            return defaultTemplates.getTemplate(type);
        }

        // Built-in fallback
        return getBuiltInDefault(type);
    }

    public TemplateConfig.EventTemplate getBuiltInDefault(GameEventType type) {
        return switch (type) {
            case PLAYER_CONNECT -> new TemplateConfig.EventTemplate(true,
                    "{{server_name}} - Player Joined", "**{{player_name}}** joined the server",
                    Map.of("priority", "default", "tags", "video_game,arrow_right", "color", "#3498db"));
            case PLAYER_DISCONNECT -> new TemplateConfig.EventTemplate(true,
                    "{{server_name}} - Player Left", "**{{player_name}}** left the server",
                    Map.of("priority", "default", "tags", "video_game,arrow_left", "color", "#95a5a6"));
            case PLAYER_DEATH -> new TemplateConfig.EventTemplate(true,
                    "{{server_name}} - Player Died", "{{death_message}}",
                    Map.of("priority", "default", "tags", "skull,video_game", "color", "#e74c3c"));
            case PLAYER_ADVANCEMENT -> new TemplateConfig.EventTemplate(true,
                    "{{server_name}} - Advancement", "{{advancement_message}}",
                    Map.of("priority", "default", "tags", "trophy,video_game", "color", "#f1c40f"));
            case PLAYER_KICKED -> new TemplateConfig.EventTemplate(true,
                    "{{server_name}} - Player Kicked", "**{{player_name}}** was kicked. Reason: {{reason}}",
                    Map.of("priority", "default", "tags", "boot,video_game", "color", "#e67e22"));
            case PLAYER_BANNED -> new TemplateConfig.EventTemplate(true,
                    "{{server_name}} - Player Banned", "**{{player_name}}** was banned by {{banned_by}}. Reason: {{reason}}",
                    Map.of("priority", "high", "tags", "hammer,warning", "color", "#c0392b"));
            case PLAYER_PARDONED -> new TemplateConfig.EventTemplate(true,
                    "{{server_name}} - Player Pardoned", "**{{player_name}}** was unbanned",
                    Map.of("priority", "default", "tags", "white_check_mark,video_game", "color", "#2ecc71"));
            case PLAYER_OP -> new TemplateConfig.EventTemplate(true,
                    "{{server_name}} - Player OP", "**{{player_name}}** is now an operator",
                    Map.of("priority", "high", "tags", "crown,video_game", "color", "#f1c40f"));
            case PLAYER_DEOP -> new TemplateConfig.EventTemplate(true,
                    "{{server_name}} - Player De-OP", "**{{player_name}}** is no longer an operator",
                    Map.of("priority", "high", "tags", "person,video_game", "color", "#95a5a6"));
            case PLAYER_WHITELISTED -> new TemplateConfig.EventTemplate(true,
                    "{{server_name}} - Player Whitelisted", "**{{player_name}}** was added to the whitelist",
                    Map.of("priority", "default", "tags", "white_check_mark,video_game", "color", "#2ecc71"));
            case PLAYER_UNWHITELISTED -> new TemplateConfig.EventTemplate(true,
                    "{{server_name}} - Removed from Whitelist", "**{{player_name}}** was removed from the whitelist",
                    Map.of("priority", "default", "tags", "x,video_game", "color", "#e74c3c"));
            case SERVER_STARTUP -> new TemplateConfig.EventTemplate(true,
                    "{{server_name}} - Started", "Server is now online!",
                    Map.of("priority", "high", "tags", "white_check_mark,video_game", "color", "#2ecc71"));
            case SERVER_SHUTDOWN -> new TemplateConfig.EventTemplate(true,
                    "{{server_name}} - Stopped", "Server has shut down.",
                    Map.of("priority", "high", "tags", "octagonal_sign,video_game", "color", "#e74c3c"));
            case SERVER_WHITELIST_ON -> new TemplateConfig.EventTemplate(true,
                    "{{server_name}} - Whitelist On", "Server whitelist has been enabled",
                    Map.of("priority", "default", "tags", "lock,video_game", "color", "#f39c12"));
            case SERVER_WHITELIST_OFF -> new TemplateConfig.EventTemplate(true,
                    "{{server_name}} - Whitelist Off", "Server whitelist has been disabled",
                    Map.of("priority", "default", "tags", "unlock,video_game", "color", "#27ae60"));
        };
    }

    private String getDefaultTemplateContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                # Default event templates
                # These are used for all targets unless overridden by target-specific templates
                #
                # Standard fields:
                #   enabled: true/false - whether to send notifications for this event
                #   title: notification title (supports placeholders)
                #   message: notification body (supports placeholders)
                #
                # Target-specific extras:
                #   ntfy - priority: min, low, default, high, urgent (see https://docs.ntfy.sh/publish/#message-priority)
                #   ntfy - tags: comma-separated emoji tags (see https://docs.ntfy.sh/emojis/)
                #   discord - color: embed color as hex (#3498db) or decimal (3447003)
                #
                # Available placeholders per event:
                #   All events: {{server_name}}
                #   player_connect: {{player_name}}, {{player_uuid}}
                #   player_disconnect: {{player_name}}, {{player_uuid}}
                #   player_death: {{player_name}}, {{player_uuid}}, {{death_message}}
                #   player_advancement: {{player_name}}, {{player_uuid}}, {{advancement_title}}, {{advancement_description}}, {{advancement_message}}
                #   player_kicked: {{player_name}}, {{player_uuid}}, {{reason}}
                #   player_banned: {{player_name}}, {{player_uuid}}, {{banned_by}}, {{reason}}
                #   player_pardoned: {{player_name}}, {{player_uuid}}
                #   player_op: {{player_name}}, {{player_uuid}}
                #   player_deop: {{player_name}}, {{player_uuid}}
                #   player_whitelisted: {{player_name}}, {{player_uuid}}
                #   player_unwhitelisted: {{player_name}}, {{player_uuid}}
                #   server_startup: (no additional placeholders)
                #   server_shutdown: (no additional placeholders)
                #   server_whitelist_on: (no additional placeholders)
                #   server_whitelist_off: (no additional placeholders)

                """);

        // Generate YAML from built-in defaults
        for (GameEventType type : GameEventType.values()) {
            TemplateConfig.EventTemplate template = getBuiltInDefault(type);
            sb.append(type.getConfigKey()).append(":\n");
            sb.append("  enabled: ").append(template.enabled()).append("\n");
            sb.append("  title: \"").append(template.title()).append("\"\n");
            sb.append("  message: \"").append(template.message()).append("\"\n");
            for (Map.Entry<String, Object> extra : template.extras().entrySet()) {
                sb.append("  ").append(extra.getKey()).append(": \"").append(extra.getValue()).append("\"\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
