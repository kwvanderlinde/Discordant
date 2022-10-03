package com.kwvanderlinde.discordant.mc;

import com.kwvanderlinde.discordant.mc.discord.DiscordantModInitializer;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

public class ProfileLinkCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("discord").then(Commands.literal("link").executes(context -> {
            final var source = context.getSource();
            final var player = source.getPlayerOrException();
            final int authCode = DiscordantModInitializer.core.generateLinkCode(
                    player.getUUID(),
                    player.getScoreboardName()
            );

            final var cfg = DiscordantModInitializer.core.getConfig();
            final var msg1 = cfg.cLinkMsg1.replaceAll("\\{botname}", DiscordantModInitializer.core.getBotName());
            final var msg2 = cfg.cLinkMsg2.replaceAll("\\{botname}", DiscordantModInitializer.core.getBotName());
            source.sendSuccess(Component.literal(msg1)
                                        .withStyle(ChatFormatting.WHITE)
                                        .append(
                                                Component.literal(String.valueOf(authCode))
                                                         .withStyle(style -> style.withColor(ChatFormatting.GREEN).withHoverEvent(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.copy.click")))
                                                                                                                                                                                                                         .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.valueOf(authCode))))).append(msg2).withStyle(ChatFormatting.WHITE), false);
            return 0;
        })));
        dispatcher.register(Commands.literal("discord").then(Commands.literal("unlink").executes(context -> {
            final var source = context.getSource();
            final var id = source.getPlayerOrException().getUUID();
            final var wasDeleted = DiscordantModInitializer.core.removeLinkedProfile(id);
            if (wasDeleted) {
                source.sendSuccess(Component.literal(DiscordantModInitializer.core.getConfig().codeUnlinkMsg), false);
            }
            else {
                source.sendFailure(Component.literal(DiscordantModInitializer.core.getConfig().codeUnlinkFail));
            }

            return 0;
        })));
    }
}
