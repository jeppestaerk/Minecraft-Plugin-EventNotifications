package dev.valhal.minecraft.plugin.EventNotifications.paper.command;

import dev.valhal.minecraft.plugin.EventNotifications.core.command.CommandHandler;
import dev.valhal.minecraft.plugin.EventNotifications.core.command.CommandResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Paper/Bukkit command adapter.
 * Implements CommandExecutor and TabCompleter for /eventnotifications command.
 */
public class PaperCommandAdapter implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "eventnotifications.admin";
    private static final List<String> SUBCOMMANDS = List.of("reload", "list", "info", "enable", "disable", "set", "message", "help");

    private final CommandHandler commandHandler;

    public PaperCommandAdapter(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendResult(sender, commandHandler.help());
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload" -> sendResult(sender, commandHandler.reload());
            case "list" -> sendResult(sender, commandHandler.list());
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /" + label + " info <target>", NamedTextColor.RED));
                    return true;
                }
                sendResult(sender, commandHandler.info(args[1]));
            }
            case "enable" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /" + label + " enable <target>", NamedTextColor.RED));
                    return true;
                }
                sendResult(sender, commandHandler.enable(args[1]));
            }
            case "disable" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /" + label + " disable <target>", NamedTextColor.RED));
                    return true;
                }
                sendResult(sender, commandHandler.disable(args[1]));
            }
            case "set" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Usage: /" + label + " set <target> <property> <value>", NamedTextColor.RED));
                    return true;
                }
                String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                sendResult(sender, commandHandler.set(args[1], args[2], value));
            }
            case "message" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /" + label + " message <target> <message>", NamedTextColor.RED));
                    return true;
                }
                String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                sender.sendMessage(Component.text("Sending message to " + args[1] + "...", NamedTextColor.GRAY));

                commandHandler.message(args[1], message).thenAccept(result -> {
                    // Bukkit async handling
                    org.bukkit.Bukkit.getScheduler().runTask(
                            org.bukkit.Bukkit.getPluginManager().getPlugin("EventNotifications"),
                            () -> sendResult(sender, result)
                    );
                });
            }
            case "help" -> sendResult(sender, commandHandler.help());
            default -> {
                sender.sendMessage(Component.text("Unknown subcommand: " + subCommand, NamedTextColor.RED));
                sendResult(sender, commandHandler.help());
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return List.of();
        }

        if (args.length == 1) {
            // Suggest subcommands
            return filterStartsWith(SUBCOMMANDS, args[0]);
        }

        String subCommand = args[0].toLowerCase();

        if (args.length == 2) {
            // Suggest target names for commands that need them
            if (List.of("info", "enable", "disable", "set", "message").contains(subCommand)) {
                return filterStartsWith(commandHandler.getTargetNames(), args[1]);
            }
        }

        if (args.length == 3 && subCommand.equals("set")) {
            // Suggest properties for the specified target
            String targetName = args[1];
            return filterStartsWith(commandHandler.getPropertiesForTarget(targetName), args[2]);
        }

        return List.of();
    }

    private List<String> filterStartsWith(List<String> options, String prefix) {
        String lowerPrefix = prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lowerPrefix)) {
                result.add(option);
            }
        }
        return result;
    }

    private void sendResult(CommandSender sender, CommandResult result) {
        switch (result) {
            case CommandResult.Success success -> {
                sender.sendMessage(Component.text(success.message()));
            }
            case CommandResult.Error error -> {
                sender.sendMessage(Component.text(error.message(), NamedTextColor.RED));
            }
            case CommandResult.TargetList list -> {
                if (list.targets().isEmpty()) {
                    sender.sendMessage(Component.text("No notification targets configured."));
                } else {
                    sender.sendMessage(Component.text("Notification Targets:", NamedTextColor.GOLD));
                    for (CommandResult.TargetList.TargetSummary target : list.targets()) {
                        Component status = target.enabled()
                                ? Component.text("[ON] ", NamedTextColor.GREEN)
                                : Component.text("[OFF] ", NamedTextColor.RED);
                        sender.sendMessage(Component.text("  ")
                                .append(status)
                                .append(Component.text(target.name(), NamedTextColor.WHITE))
                                .append(Component.text(" (" + target.type() + ")", NamedTextColor.GRAY)));
                    }
                }
            }
            case CommandResult.TargetInfo info -> {
                Component status = info.enabled()
                        ? Component.text("Enabled", NamedTextColor.GREEN)
                        : Component.text("Disabled", NamedTextColor.RED);
                sender.sendMessage(Component.text("Target: ", NamedTextColor.WHITE, TextDecoration.BOLD)
                        .append(Component.text(info.name()).decoration(TextDecoration.BOLD, false)));
                sender.sendMessage(Component.text("  Type: " + info.type()));
                sender.sendMessage(Component.text("  Status: ").append(status));
                if (!info.properties().isEmpty()) {
                    sender.sendMessage(Component.text("  Properties:"));
                    for (var entry : info.properties().entrySet()) {
                        String displayValue = maskSensitiveValue(entry.getKey(), entry.getValue());
                        sender.sendMessage(Component.text("    " + entry.getKey() + ": " + displayValue));
                    }
                }
            }
            case CommandResult.MessageSent sent -> {
                if (sent.success()) {
                    sender.sendMessage(Component.text("Message sent to " + sent.targetName() + ": " + sent.resultMessage(), NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Failed to send message to " + sent.targetName() + ": " + sent.resultMessage(), NamedTextColor.RED));
                }
            }
        }
    }

    private String maskSensitiveValue(String key, String value) {
        if (key.toLowerCase().contains("token") ||
                key.toLowerCase().contains("password") ||
                key.toLowerCase().contains("secret") ||
                key.toLowerCase().contains("webhook_url")) {
            if (value != null && value.length() > 8) {
                return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
            }
            return "****";
        }
        return value;
    }
}
