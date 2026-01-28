package dev.valhal.minecraft.plugin.EventNotifications.core.command;

import dev.valhal.minecraft.plugin.EventNotifications.core.config.TargetConfig;

import java.util.List;
import java.util.Map;

/**
 * Represents the result of executing a command.
 * Used to communicate results from CommandHandler to platform-specific adapters.
 */
public sealed interface CommandResult {

    /**
     * Command executed successfully with a message.
     */
    record Success(String message) implements CommandResult {}

    /**
     * Command failed with an error message.
     */
    record Error(String message) implements CommandResult {}

    /**
     * Result containing a list of targets with their status.
     */
    record TargetList(List<TargetSummary> targets) implements CommandResult {
        public record TargetSummary(String name, String type, boolean enabled) {}
    }

    /**
     * Result containing detailed information about a single target.
     */
    record TargetInfo(
            String name,
            String type,
            boolean enabled,
            Map<String, String> properties
    ) implements CommandResult {}

    /**
     * Result of sending a custom message to a target.
     */
    record MessageSent(String targetName, boolean success, String resultMessage) implements CommandResult {}

    // Factory methods for convenience
    static Success success(String message) {
        return new Success(message);
    }

    static Error error(String message) {
        return new Error(message);
    }

    static TargetList targetList(List<TargetConfig> configs) {
        List<TargetList.TargetSummary> summaries = configs.stream()
                .map(c -> new TargetList.TargetSummary(c.name(), c.type(), c.enabled()))
                .toList();
        return new TargetList(summaries);
    }

    static TargetInfo targetInfo(TargetConfig config) {
        Map<String, String> props = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : config.properties().entrySet()) {
            props.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return new TargetInfo(config.name(), config.type(), config.enabled(), props);
    }

    static MessageSent messageSent(String targetName, boolean success, String resultMessage) {
        return new MessageSent(targetName, success, resultMessage);
    }
}
