package com.kwvanderlinde.discordant.mc;

import com.kwvanderlinde.discordant.core.messages.scopes.NilScope;
import com.kwvanderlinde.discordant.core.messages.scopes.PendingVerificationScope;
import com.kwvanderlinde.discordant.mc.discord.DiscordantModInitializer;
import com.kwvanderlinde.discordant.mc.messages.SemanticMessageRenderer;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ProfileLinkCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("discord").then(Commands.literal("link").executes(context -> {
            // TODO If already linked, tell the user instead of generating a new code.
            final var source = context.getSource();
            final var player = source.getPlayerOrException();
            final int authCode = DiscordantModInitializer.core.generateLinkCode(
                    player.getUUID(),
                    player.getScoreboardName()
            );

            final var messageConfig = DiscordantModInitializer.core.getMessageConfig();
            final var component = new PendingVerificationScope(String.valueOf(authCode), DiscordantModInitializer.core.getBotName())
                    .instantiate(messageConfig.commandLinkMsg)
                    .reduce(SemanticMessageRenderer::renderMessage);
            source.sendSuccess(component, false);
            return 0;
        })));
        dispatcher.register(Commands.literal("discord").then(Commands.literal("unlink").executes(context -> {
            final var source = context.getSource();
            final var id = source.getPlayerOrException().getUUID();
            final var wasDeleted = DiscordantModInitializer.core.removeLinkedProfile(id);
            if (wasDeleted) {
                source.sendSuccess(new NilScope().instantiate(DiscordantModInitializer.core.getMessageConfig().codeUnlinkMsg)
                                                 .reduce(SemanticMessageRenderer::renderMessage), false);
            }
            else {
                source.sendFailure(new NilScope().instantiate(DiscordantModInitializer.core.getMessageConfig().codeUnlinkFail)
                                                 .reduce(SemanticMessageRenderer::renderMessage));
            }

            return 0;
        })));
    }
}
