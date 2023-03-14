package com.kwvanderlinde.discordant.mc;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;
import com.kwvanderlinde.discordant.core.modinterfaces.CommandEventHandler;
import com.kwvanderlinde.discordant.mc.integration.PlayerAdapter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class DiscordantCommands {
    private record CommandSourceStackResponder(CommandSourceStack source) implements CommandEventHandler.Responder {
        @Override
        public void success(SemanticMessage message) {
            source.sendSuccess(message.reduce(ComponentRenderer.instance()), false);
        }

        @Override
        public void failure(SemanticMessage message) {
            source.sendFailure(message.reduce(ComponentRenderer.instance()));
        }
    }

    private boolean linkingEnabled = false;

    public void setLinkingCommandsEnabled(boolean linkingEnabled) {
        this.linkingEnabled = linkingEnabled;
    }

    public void register(CommandEventHandler commandEventHandler, CommandDispatcher<CommandSourceStack> dispatcher) {
        final var commands = Commands.literal("discord");

        // Toggle notification sounds.
        commands.then(Commands.literal("enable-mention-notification")
                              .executes(context -> sendNotifySoundState(context.getSource(), commandEventHandler))
                              .then(Commands.argument("soundstate", BoolArgumentType.bool())
                                            .executes(context -> switchNotifySoundState(context.getSource(), BoolArgumentType.getBool(context, "soundstate"), commandEventHandler))));

        // Linking commands only if linking is a possibility.
        commands.then(Commands.literal("link")
                              .requires(s -> linkingEnabled)
                              .executes(context -> link(context.getSource(), commandEventHandler)))
                .then(Commands.literal("unlink")
                              .requires(s -> linkingEnabled)
                              .executes(context -> unlink(context.getSource(), commandEventHandler)));

        commands.then(Commands.literal("reload")
                              .requires(s -> s.hasPermission(4))
                              .executes(context -> { commandEventHandler.onReload(); return 0; }));

        dispatcher.register(commands);
    }

    private static int link(CommandSourceStack source, CommandEventHandler handler) throws CommandSyntaxException {
        final var player = source.getPlayerOrException();
        handler.onLink(new PlayerAdapter(player), new CommandSourceStackResponder(source));
        return 0;
    }

    private static int unlink(CommandSourceStack source, CommandEventHandler handler) throws CommandSyntaxException {
        final var player = source.getPlayerOrException();
        handler.onUnlink(new PlayerAdapter(player), new CommandSourceStackResponder(source));
        return 0;
    }

    private static int sendNotifySoundState(CommandSourceStack source, CommandEventHandler handler) throws CommandSyntaxException {
        final var player = source.getPlayerOrException();
        handler.onQueryMentionNotificationsEnabled(new PlayerAdapter(player), new CommandSourceStackResponder(source));
        return 0;
    }

    private static int switchNotifySoundState(CommandSourceStack source, boolean state, CommandEventHandler handler) throws CommandSyntaxException {
        final var player = source.getPlayerOrException();
        handler.onSetMentionNotificationsEnabled(new PlayerAdapter(player), state, new CommandSourceStackResponder(source));
        return 0;
    }
}
