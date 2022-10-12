package com.kwvanderlinde.discordant.mc;

import com.kwvanderlinde.discordant.core.messages.scopes.NotificationStateScope;
import com.kwvanderlinde.discordant.mc.discord.DiscordantModInitializer;
import com.kwvanderlinde.discordant.mc.messages.ComponentRenderer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class DiscordantCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        final var commands =
                Commands.literal("discord")
                        .then(Commands.literal("enable-mention-notification")
                                      .executes(context -> sendNotifySoundState(context.getSource()))
                                      .then(Commands.argument("soundstate", BoolArgumentType.bool())
                                                    .executes(context -> switchNotifySoundState(context.getSource(), BoolArgumentType.getBool(context, "soundstate"))))
                                     );
        dispatcher.register(commands);
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
