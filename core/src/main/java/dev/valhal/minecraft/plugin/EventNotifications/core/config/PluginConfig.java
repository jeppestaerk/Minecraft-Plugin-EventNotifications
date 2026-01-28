package dev.valhal.minecraft.plugin.EventNotifications.core.config;

import java.util.List;

public record PluginConfig(
        GeneralConfig general,
        List<TargetConfig> targets
) {
    // Convenience accessors for backwards compatibility
    public String serverName() {
        return general.serverName();
    }

    public boolean commandsEnabled() {
        return general.commandsEnabled();
    }

    public String commandAlias() {
        return general.commandAlias();
    }
}
