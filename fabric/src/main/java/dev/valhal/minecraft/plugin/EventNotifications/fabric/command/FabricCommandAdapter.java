package dev.valhal.minecraft.plugin.EventNotifications.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.valhal.minecraft.plugin.EventNotifications.core.command.CommandHandler;
import dev.valhal.minecraft.plugin.EventNotifications.core.command.CommandResult;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Fabric command adapter using Brigadier.
 * Registers /eventnotifications and optional alias command.
 */
public class FabricCommandAdapter {
    private final CommandHandler commandHandler;
    private final String commandAlias;

    public FabricCommandAdapter(CommandHandler commandHandler, String commandAlias) {
        this.commandHandler = commandHandler;
        this.commandAlias = commandAlias;
    }

    public void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerCommands(dispatcher, "eventnotifications");
            // Register alias if configured
            if (commandAlias != null && !commandAlias.isBlank()) {
                registerCommands(dispatcher, commandAlias);
            }
        });
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(
                Commands.literal(commandName)
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)) // OP level 2+
                        .executes(this::executeHelp)
                        .then(Commands.literal("reload")
                                .executes(this::executeReload))
                        .then(Commands.literal("list")
                                .executes(this::executeList))
                        .then(Commands.literal("info")
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests(suggestTargets())
                                        .executes(this::executeInfo)))
                        .then(Commands.literal("enable")
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests(suggestTargets())
                                        .executes(this::executeEnable)))
                        .then(Commands.literal("disable")
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests(suggestTargets())
                                        .executes(this::executeDisable)))
                        .then(Commands.literal("set")
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests(suggestTargets())
                                        .then(Commands.argument("property", StringArgumentType.word())
                                                .suggests(suggestProperties())
                                                .then(Commands.argument("value", StringArgumentType.greedyString())
                                                        .executes(this::executeSet)))))
                        .then(Commands.literal("message")
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests(suggestTargets())
                                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                                .executes(this::executeMessage))))
                        .then(Commands.literal("help")
                                .executes(this::executeHelp))
        );
    }

    // ========== Suggestion Providers ==========

    private SuggestionProvider<CommandSourceStack> suggestTargets() {
        return (context, builder) -> {
            List<String> targets = commandHandler.getTargetNames();
            for (String target : targets) {
                if (target.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                    builder.suggest(target);
                }
            }
            return builder.buildFuture();
        };
    }

    private SuggestionProvider<CommandSourceStack> suggestProperties() {
        return (context, builder) -> {
            String targetName = StringArgumentType.getString(context, "target");
            List<String> properties = commandHandler.getPropertiesForTarget(targetName);
            for (String property : properties) {
                if (property.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                    builder.suggest(property);
                }
            }
            return builder.buildFuture();
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
        source.sendSuccess(() -> Component.literal("Sending message to " + targetName + "..."), false);

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
                source.sendSuccess(() -> Component.literal(success.message()), false);
            }
            case CommandResult.Error error -> {
                source.sendFailure(Component.literal(error.message()));
            }
            case CommandResult.TargetList list -> {
                if (list.targets().isEmpty()) {
                    source.sendSuccess(() -> Component.literal("No notification targets configured."), false);
                } else {
                    source.sendSuccess(() -> Component.literal("Notification Targets:"), false);
                    for (CommandResult.TargetList.TargetSummary target : list.targets()) {
                        String status = target.enabled() ? "\u00a7a[ON]" : "\u00a7c[OFF]";
                        source.sendSuccess(() -> Component.literal(
                                "  " + status + " \u00a7r" + target.name() + " \u00a77(" + target.type() + ")"
                        ), false);
                    }
                }
            }
            case CommandResult.TargetInfo info -> {
                String status = info.enabled() ? "\u00a7aEnabled" : "\u00a7cDisabled";
                source.sendSuccess(() -> Component.literal("\u00a7lTarget: \u00a7r" + info.name()), false);
                source.sendSuccess(() -> Component.literal("  Type: " + info.type()), false);
                source.sendSuccess(() -> Component.literal("  Status: " + status), false);
                if (!info.properties().isEmpty()) {
                    source.sendSuccess(() -> Component.literal("  Properties:"), false);
                    for (var entry : info.properties().entrySet()) {
                        String displayValue = maskSensitiveValue(entry.getKey(), entry.getValue());
                        source.sendSuccess(() -> Component.literal(
                                "    " + entry.getKey() + ": " + displayValue
                        ), false);
                    }
                }
            }
            case CommandResult.MessageSent sent -> {
                if (sent.success()) {
                    source.sendSuccess(() -> Component.literal(
                            "\u00a7aMessage sent to " + sent.targetName() + ": " + sent.resultMessage()
                    ), false);
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
