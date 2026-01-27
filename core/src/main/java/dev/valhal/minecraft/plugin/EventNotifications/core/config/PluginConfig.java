package dev.valhal.minecraft.plugin.EventNotifications.core.config;

import java.util.List;

public record PluginConfig(
        String serverName,
        List<TargetConfig> targets
) {
}
