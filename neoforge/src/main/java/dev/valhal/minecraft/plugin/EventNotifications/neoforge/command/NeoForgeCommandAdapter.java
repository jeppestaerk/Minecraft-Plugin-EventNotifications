package dev.valhal.minecraft.plugin.EventNotifications.neoforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.valhal.minecraft.plugin.EventNotifications.core.command.CommandHandler;
import dev.valhal.minecraft.plugin.EventNotifications.core.command.CommandResult;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * NeoForge command adapter using Brigadier.
 * Registers /eventnotifications and optional alias command via RegisterCommandsEvent.
 */
public class NeoForgeCommandAdapter {
    private final CommandHandler commandHandler;
    private final String commandAlias;

    public NeoForgeCommandAdapter(CommandHandler commandHandler, String commandAlias) {
        this.commandHandler = commandHandler;
        this.commandAlias = commandAlias;
    }

    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        registerCommands(dispatcher, "eventnotifications");
        // Register alias if configured
        if (commandAlias != null && !commandAlias.isBlank()) {
            registerCommands(dispatcher, commandAlias);
        }
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(
                literal(commandName)
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)) // OP level 2+
                        .executes(this::executeHelp)
                        .then(literal("reload")
                                .executes(this::executeReload))
                        .then(literal("list")
                                .executes(this::executeList))
                        .then(literal("info")
                                .then(argument("target", StringArgumentType.word())
                                        .suggests(suggestTargets())
                                        .executes(this::executeInfo)))
                        .then(literal("enable")
                                .then(argument("target", StringArgumentType.word())
                                        .suggests(suggestTargets())
                                        .executes(this::executeEnable)))
                        .then(literal("disable")
                                .then(argument("target", StringArgumentType.word())
                                        .suggests(suggestTargets())
                                        .executes(this::executeDisable)))
                        .then(literal("set")
                                .then(argument("target", StringArgumentType.word())
                                        .suggests(suggestTargets())
                                        .then(argument("property", StringArgumentType.word())
                                                .suggests(suggestProperties())
                                                .then(argument("value", StringArgumentType.greedyString())
                                                        .executes(this::executeSet)))))
                        .then(literal("message")
                                .then(argument("target", StringArgumentType.word())
                                        .suggests(suggestTargets())
                                        .then(argument("message", StringArgumentType.greedyString())
                                                .executes(this::executeMessage))))
                        .then(literal("help")
                                .executes(this::executeHelp))
        );
    }

    // ========== Suggestion Providers ==========

    private SuggestionProvider<CommandSourceStack> suggestTargets() {
        return (context, builder) -> {
            List<String> targets = commandHandler.getTargetNames();
            return SharedSuggestionProvider.suggest(targets, builder);
        };
    }

    private SuggestionProvider<CommandSourceStack> suggestProperties() {
        return (context, builder) -> {
            String targetName = StringArgumentType.getString(context, "target");
            List<String> properties = commandHandler.getPropertiesForTarget(targetName);
            return SharedSuggestionProvider.suggest(properties, builder);
        };
    }

    // ========== Command Executors ==========

    private int executeHelp(CommandContext<CommandSourceStack> context) {
        CommandResult result = commandHandler.help();
        sendResult(context.getSource(), result);
        return 1;
    }

    private int executeReload(CommandContext<CommandSourceStack> context) {
        CommandResult result = commandHandler.reload();
        sendResult(context.getSource(), result);
        return result instanceof CommandResult.Success ? 1 : 0;
    }

    private int executeList(CommandContext<CommandSourceStack> context) {
        CommandResult result = commandHandler.list();
        sendResult(context.getSource(), result);
        return 1;
    }

    private int executeInfo(CommandContext<CommandSourceStack> context) {
        String targetName = StringArgumentType.getString(context, "target");
        CommandResult result = commandHandler.info(targetName);
        sendResult(context.getSource(), result);
        return result instanceof CommandResult.Error ? 0 : 1;
    }

    private int executeEnable(CommandContext<CommandSourceStack> context) {
        String targetName = StringArgumentType.getString(context, "target");
        CommandResult result = commandHandler.enable(targetName);
        sendResult(context.getSource(), result);
        return result instanceof CommandResult.Success ? 1 : 0;
    }

    private int executeDisable(CommandContext<CommandSourceStack> context) {
        String targetName = StringArgumentType.getString(context, "target");
        CommandResult result = commandHandler.disable(targetName);
        sendResult(context.getSource(), result);
        return result instanceof CommandResult.Success ? 1 : 0;
    }

    private int executeSet(CommandContext<CommandSourceStack> context) {
        String targetName = StringArgumentType.getString(context, "target");
        String property = StringArgumentType.getString(context, "property");
        String value = StringArgumentType.getString(context, "value");
        CommandResult result = commandHandler.set(targetName, property, value);
        sendResult(context.getSource(), result);
        return result instanceof CommandResult.Success ? 1 : 0;
    }

    private int executeMessage(CommandContext<CommandSourceStack> context) {
        String targetName = StringArgumentType.getString(context, "target");
        String message = StringArgumentType.getString(context, "message");

        CommandSourceStack source = context.getSource();
        source.sendSystemMessage(Component.literal("Sending message to " + targetName + "..."));

        commandHandler.message(targetName, message).thenAccept(result -> {
            // Run on main thread
            source.getServer().execute(() -> sendResult(source, result));
        });

        return 1;
    }

    // ========== Result Formatting ==========

    private void sendResult(CommandSourceStack source, CommandResult result) {
        switch (result) {
            case CommandResult.Success success -> {
                source.sendSystemMessage(Component.literal(success.message()));
            }
            case CommandResult.Error error -> {
                source.sendFailure(Component.literal(error.message()));
            }
            case CommandResult.TargetList list -> {
                if (list.targets().isEmpty()) {
                    source.sendSystemMessage(Component.literal("No notification targets configured."));
                } else {
                    source.sendSystemMessage(Component.literal("Notification Targets:"));
                    for (CommandResult.TargetList.TargetSummary target : list.targets()) {
                        String status = target.enabled() ? "\u00a7a[ON]" : "\u00a7c[OFF]";
                        source.sendSystemMessage(Component.literal(
                                "  " + status + " \u00a7r" + target.name() + " \u00a77(" + target.type() + ")"
                        ));
                    }
                }
            }
            case CommandResult.TargetInfo info -> {
                String status = info.enabled() ? "\u00a7aEnabled" : "\u00a7cDisabled";
                source.sendSystemMessage(Component.literal("\u00a7lTarget: \u00a7r" + info.name()));
                source.sendSystemMessage(Component.literal("  Type: " + info.type()));
                source.sendSystemMessage(Component.literal("  Status: " + status));
                if (!info.properties().isEmpty()) {
                    source.sendSystemMessage(Component.literal("  Properties:"));
                    for (var entry : info.properties().entrySet()) {
                        String displayValue = maskSensitiveValue(entry.getKey(), entry.getValue());
                        source.sendSystemMessage(Component.literal(
                                "    " + entry.getKey() + ": " + displayValue
                        ));
                    }
                }
            }
            case CommandResult.MessageSent sent -> {
                if (sent.success()) {
                    source.sendSystemMessage(Component.literal(
                            "\u00a7aMessage sent to " + sent.targetName() + ": " + sent.resultMessage()
                    ));
                } else {
                    source.sendFailure(Component.literal(
                            "Failed to send message to " + sent.targetName() + ": " + sent.resultMessage()
                    ));
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
