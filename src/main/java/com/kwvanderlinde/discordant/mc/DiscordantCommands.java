package com.kwvanderlinde.discordant.mc;

import com.kwvanderlinde.discordant.core.messages.scopes.NilScope;
import com.kwvanderlinde.discordant.core.messages.scopes.NotificationStateScope;
import com.kwvanderlinde.discordant.core.messages.scopes.PendingVerificationScope;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class DiscordantCommands {
    // TODO Move business logic out of here and into core.

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, boolean linkingEnabled) {
        final var commands = Commands.literal("discord");

        // Toggle notification sounds.
        commands.then(Commands.literal("enable-mention-notification")
                              .executes(context -> sendNotifySoundState(context.getSource()))
                              .then(Commands.argument("soundstate", BoolArgumentType.bool())
                                            .executes(context -> switchNotifySoundState(context.getSource(), BoolArgumentType.getBool(context, "soundstate")))));

        // Linking commands only if linking is a possibility.
        if (linkingEnabled) {
            commands.then(Commands.literal("link")
                                  .executes(context -> link(context.getSource())))
                    .then(Commands.literal("unlink")
                                  .executes(context -> unlink(context.getSource())));
        }

        dispatcher.register(commands);
    }

    private static int link(CommandSourceStack source) throws CommandSyntaxException {
        // TODO If already linked, tell the user instead of generating a new code.
        final var player = source.getPlayerOrException();
        final int authCode = DiscordantModInitializer.core.generateLinkCode(
                player.getUUID(),
                player.getScoreboardName()
        );

        final var config = DiscordantModInitializer.core.getConfig();
        final var component = config.minecraft.messages.commandLinkMsg
                .instantiate(new PendingVerificationScope(String.valueOf(authCode), DiscordantModInitializer.core.getBotName()))
                .reduce(ComponentRenderer.instance());
        source.sendSuccess(component, false);
        return 0;
    }

    private static int unlink(CommandSourceStack source) throws CommandSyntaxException {
        final var id = source.getPlayerOrException().getUUID();
        final var wasDeleted = DiscordantModInitializer.core.removeLinkedProfile(id);
        final var config = DiscordantModInitializer.core.getConfig();
        if (wasDeleted) {
            final var component = config.minecraft.messages.codeUnlinkMsg
                    .instantiate(new NilScope())
                    .reduce(ComponentRenderer.instance());
            source.sendSuccess(component, false);
        }
        else {
            final var component = config.minecraft.messages.codeUnlinkFail
                    .instantiate(new NilScope())
                    .reduce(ComponentRenderer.instance());
            source.sendFailure(component);
        }

        return 0;
    }

    private static int sendNotifySoundState(CommandSourceStack source) throws CommandSyntaxException {
        IServerPlayer player = (IServerPlayer) source.getPlayerOrException();
        final var state = player.getNotifyState();
        final var config = DiscordantModInitializer.core.getConfig();
        final var component = config.minecraft.messages.mentionStateQueryResponse
                .instantiate(new NotificationStateScope(state))
                .reduce(ComponentRenderer.instance());
        source.sendSuccess(component, false);
        return 0;
    }

    private static int switchNotifySoundState(CommandSourceStack source, boolean state) throws CommandSyntaxException {
        IServerPlayer player = (IServerPlayer) source.getPlayerOrException();
        player.setNotifyState(state);
        final var config = DiscordantModInitializer.core.getConfig();
        final var component = config.minecraft.messages.mentionStateUpdateResponse
                        .instantiate(new NotificationStateScope(state))
                        .reduce(ComponentRenderer.instance());
        source.sendSuccess(component, false);
        return 0;
    }
}
