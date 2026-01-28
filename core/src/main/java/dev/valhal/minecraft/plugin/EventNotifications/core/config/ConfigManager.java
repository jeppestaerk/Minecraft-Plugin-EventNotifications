package dev.valhal.minecraft.plugin.EventNotifications.core.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Manages plugin configuration with load/save/update capabilities.
 * Wraps ConfigLoader and maintains current config state.
 */
public class ConfigManager {
    private final ConfigLoader configLoader;
    private final Consumer<String> logger;
    private PluginConfig currentConfig;

    public ConfigManager(ConfigLoader configLoader, Consumer<String> logger) {
        this.configLoader = configLoader;
        this.logger = logger;
    }

    public PluginConfig load() throws IOException {
        currentConfig = configLoader.load();
        return currentConfig;
    }

    public PluginConfig reload() throws IOException {
        return load();
    }

    public void save() throws IOException {
        if (currentConfig == null) {
            throw new IllegalStateException("No config loaded to save");
        }
        configLoader.save(currentConfig);
    }

    public PluginConfig getCurrentConfig() {
        return currentConfig;
    }

    public Optional<TargetConfig> getTarget(String name) {
        if (currentConfig == null) {
            return Optional.empty();
        }
        return currentConfig.targets().stream()
                .filter(t -> t.name().equalsIgnoreCase(name))
                .findFirst();
    }

    public List<String> getTargetNames() {
        if (currentConfig == null) {
            return List.of();
        }
        return currentConfig.targets().stream()
                .map(TargetConfig::name)
                .toList();
    }

    public boolean updateTarget(String name, TargetConfig updatedTarget) throws IOException {
        if (currentConfig == null) {
            return false;
        }

        List<TargetConfig> newTargets = new ArrayList<>();
        boolean found = false;

        for (TargetConfig target : currentConfig.targets()) {
            if (target.name().equalsIgnoreCase(name)) {
                newTargets.add(updatedTarget);
                found = true;
            } else {
                newTargets.add(target);
            }
        }

        if (found) {
            currentConfig = new PluginConfig(currentConfig.general(), newTargets);
            save();
            logger.accept("Updated target configuration: " + name);
        }

        return found;
    }

    public boolean setTargetEnabled(String name, boolean enabled) throws IOException {
        Optional<TargetConfig> target = getTarget(name);
        if (target.isEmpty()) {
            return false;
        }

        TargetConfig updated = target.get().withEnabled(enabled);
        return updateTarget(name, updated);
    }

    public boolean setTargetProperty(String name, String property, String value) throws IOException {
        Optional<TargetConfig> target = getTarget(name);
        if (target.isEmpty()) {
            return false;
        }

        // Handle 'enabled' as a special case
        if (property.equalsIgnoreCase("enabled")) {
            boolean enabled = Boolean.parseBoolean(value);
            return setTargetEnabled(name, enabled);
        }

        TargetConfig updated = target.get().withProperty(property, value);
        return updateTarget(name, updated);
    }
}
