package com.kwvanderlinde.discordant.mc;

import com.kwvanderlinde.discordant.core.messages.scopes.NotificationStateScope;
import com.kwvanderlinde.discordant.mc.discord.DiscordantModInitializer;
import com.kwvanderlinde.discordant.mc.messages.SemanticMessageRenderer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class DiscordantCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // TODO Can we not flatten this chaining?
        dispatcher.register(Commands.literal("discord").then(Commands.literal("enable-mention-notification")
                                                                     .then(Commands.argument("soundstate", BoolArgumentType.bool()).executes(context ->
                                                                                                                                                     switchNotifySoundState(context.getSource(), BoolArgumentType.getBool(context, "soundstate"))))));
    }

    private static int switchNotifySoundState(CommandSourceStack source, boolean state) throws CommandSyntaxException {
        IServerPlayer player = (IServerPlayer) source.getPlayerOrException();
        player.setNotifyState(state);
        source.sendSuccess(new NotificationStateScope(state).instantiate(DiscordantModInitializer.core.getMessageConfig().mentionState).reduce(SemanticMessageRenderer::renderMessage), false);
        return 0;
    }
}
