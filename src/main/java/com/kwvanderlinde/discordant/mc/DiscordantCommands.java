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
        // TODO Can we not flatten this chaining?
        // TODO Support form without arguments that returns the current state.
        dispatcher.register(Commands.literal("discord").then(Commands.literal("enable-mention-notification")
                                                                     .then(Commands.argument("soundstate", BoolArgumentType.bool()).executes(context ->
                                                                                                                                                     switchNotifySoundState(context.getSource(), BoolArgumentType.getBool(context, "soundstate"))))));
    }

    private static int switchNotifySoundState(CommandSourceStack source, boolean state) throws CommandSyntaxException {
        IServerPlayer player = (IServerPlayer) source.getPlayerOrException();
        player.setNotifyState(state);
        final var config = DiscordantModInitializer.core.getConfig();
        final var component = config.minecraft.messages.mentionState
                        .instantiate(new NotificationStateScope(state))
                        .reduce(ComponentRenderer.instance());
        source.sendSuccess(component, false);
        return 0;
    }
}
