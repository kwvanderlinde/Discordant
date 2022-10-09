package com.kwvanderlinde.discordant.mc;

import com.kwvanderlinde.discordant.core.messages.scopes.NilScope;
import com.kwvanderlinde.discordant.core.messages.scopes.PendingVerificationScope;
import com.kwvanderlinde.discordant.mc.discord.DiscordantModInitializer;
import com.kwvanderlinde.discordant.mc.messages.ComponentRenderer;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

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

            final var config = DiscordantModInitializer.core.getConfig();
            final var component = config.minecraft.messages.commandLinkMsg
                            .instantiate(new PendingVerificationScope(String.valueOf(authCode), DiscordantModInitializer.core.getBotName()))
                            .reduce(ComponentRenderer.instance());
            source.sendSuccess(component, false);
            return 0;
        })));
        dispatcher.register(Commands.literal("discord").then(Commands.literal("unlink").executes(context -> {
            final var source = context.getSource();
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
        })));
    }
}
