package com.kwvanderlinde.discordant.mc;

import com.kwvanderlinde.discordant.core.config.ConfigManager;
import com.kwvanderlinde.discordant.core.config.LinkedProfile;
import com.kwvanderlinde.discordant.mc.discord.DiscordConfig;
import com.kwvanderlinde.discordant.mc.discord.Discordant;
import com.kwvanderlinde.discordant.mc.discord.VerificationData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ProfileLinkCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("discord").then(Commands.literal("link").executes(context -> generateLinkCode(context.getSource()))));
        dispatcher.register(Commands.literal("discord").then(Commands.literal("unlink").executes(context -> {
            try {
                return unlink(context.getSource());
            }
            catch (IOException e) {
                e.printStackTrace();
                context.getSource().sendFailure(Component.literal("IO Exception occurred during this operation"));
            }
            return 0;
        })));
    }

    private static int generateLinkCode(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String uuid = player.getStringUUID();
        DiscordConfig cfg = Discordant.config;
        if (Discordant.pendingPlayersUUID.containsKey(uuid)) {
            int code = Discordant.pendingPlayersUUID.get(uuid);
            source.sendSuccess(Component.literal(cfg.cLinkMsg1).withStyle(ChatFormatting.WHITE).append(Component.literal(String.valueOf(code)).withStyle(style -> style.withColor(ChatFormatting.GREEN).withHoverEvent(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.copy.click")))
                                                                                                                                                                       .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.valueOf(code))))).append(cfg.cLinkMsg2).withStyle(ChatFormatting.WHITE), false);
            return 0;
        }
        RandomSource r = player.getRandom();
        int authCode = r.nextInt(100_000, 1_000_000);
        while (Discordant.pendingPlayers.containsKey(authCode)) {
            authCode = r.nextInt(100_000, 1_000_000);
        }
        Discordant.pendingPlayers.put(authCode, new VerificationData(player.getScoreboardName(), uuid, Discordant.currentTime + 600_000));
        Discordant.pendingPlayersUUID.put(uuid, authCode);
        int finalAuthCode = authCode;
        source.sendSuccess(Component.literal(cfg.cLinkMsg1.replaceAll("\\{botname}", Discordant.botName)).withStyle(ChatFormatting.WHITE).append(Component.literal(String.valueOf(authCode)).withStyle(style -> style.withColor(ChatFormatting.GREEN).withHoverEvent(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.copy.click")))
                                                                                                                                                                                                                     .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.valueOf(finalAuthCode))))).append(cfg.cLinkMsg2.replaceAll("\\{botname}", Discordant.botName)).withStyle(ChatFormatting.WHITE), false);
        return 0;
    }

    private static int unlink(CommandSourceStack source) throws CommandSyntaxException, IOException {
        String id = source.getPlayerOrException().getStringUUID();
        LinkedProfile profile = ConfigManager.getLinkedProfile(id);
        if (profile != null) {
            Discordant.linkedPlayersByDiscordId.remove(profile.discordId());
            Discordant.linkedPlayers.remove(id);
            Files.delete(Paths.get(String.format("./config/discordant/linked-profiles/%s.json", id)));
            source.sendSuccess(Component.literal(Discordant.config.codeUnlinkMsg), false);
        }
        else {
            source.sendFailure(Component.literal(Discordant.config.codeUnlinkFail));
        }
        return 0;
    }
}
