package dev.valhal.minecraft.plugin.EventNotifications.core.config;

/**
 * General configuration settings.
 */
public record GeneralConfig(
        String serverName,
        boolean commandsEnabled,
        String commandAlias
) {
    /**
     * Returns true if the command alias is configured and not empty.
     */
    public boolean hasAlias() {
        return commandAlias != null && !commandAlias.isBlank();
    }
}
