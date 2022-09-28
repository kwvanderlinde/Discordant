package com.kwvanderlinde.discordant.mixin;

import com.kwvanderlinde.discordant.config.ConfigManager;
import com.kwvanderlinde.discordant.config.LinkedProfile;
import com.kwvanderlinde.discordant.discord.DiscordConfig;
import com.kwvanderlinde.discordant.discord.Discordant;
import com.kwvanderlinde.discordant.discord.VerificationData;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Random;


@Mixin(PlayerList.class)
public class PlayerListMixins {

    private final Random r = new Random();

    @Inject(method = "placeNewPlayer", at = @At("HEAD"))
    private void sendDiscordWelcomeMsg(Connection connection, ServerPlayer serverPlayer, CallbackInfo ci) {
        Discordant.greetingMsg(serverPlayer.getScoreboardName(), serverPlayer.getStringUUID());
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void sendLogoutMessage(ServerPlayer serverPlayer, CallbackInfo ci) {
        Discordant.logoutMsg(serverPlayer.getScoreboardName(), serverPlayer.getStringUUID());
        LinkedProfile profile = Discordant.linkedPlayers.get(serverPlayer.getStringUUID());
        if (profile != null) {
            Discordant.linkedPlayersByDiscordId.remove(profile.discordId);
            Discordant.linkedPlayers.remove(serverPlayer.getGameProfile().getId().toString());
        }
    }

    @Inject(method = "canPlayerLogin", at = @At("HEAD"), cancellable = true)
    private void checkLink(SocketAddress socketAddress, GameProfile gameProfile, CallbackInfoReturnable<Component> cir) throws IOException {
        DiscordConfig cfg = Discordant.config;
        String uuid = gameProfile.getId().toString();
        LinkedProfile profile = null;
        if (cfg.enableAccountLinking) {
            profile = ConfigManager.getLinkedProfile(uuid);
            if (profile != null) {
                Discordant.linkedPlayers.put(uuid, profile);
                Discordant.linkedPlayersByDiscordId.put(profile.discordId, gameProfile.getName());
            }
        }
        if (cfg.enableAccountLinking && cfg.forceLinking && profile == null) {
            if (!Discordant.pendingPlayersUUID.containsKey(uuid)) {
                int authCode = r.nextInt(100_000, 1_000_000);
                while (Discordant.pendingPlayers.containsKey(authCode)) {
                    authCode = r.nextInt(100_000, 1_000_000);
                }
                String auth = String.valueOf(authCode);
                Discordant.pendingPlayers.put(authCode, new VerificationData(gameProfile.getName(), uuid, Discordant.currentTime + 600_000));
                Discordant.pendingPlayersUUID.put(uuid, authCode);
                cir.setReturnValue(Component.literal(cfg.vDisconnectMsg1.replaceAll("\\{botname}", Discordant.botName))
                                            .append(Component.literal(auth).withStyle(style -> style.withColor(ChatFormatting.GREEN)))
                                            .append(Component.literal(cfg.vDisconnectMsg2.replaceAll("\\{botname}", Discordant.botName)).withStyle(ChatFormatting.WHITE)));
            }
            else {
                cir.setReturnValue(Component.literal(cfg.vDisconnectMsg1.replaceAll("\\{botname}", Discordant.botName))
                                            .append(Component.literal(" " + Discordant.pendingPlayersUUID.get(uuid)).withStyle(style -> style.withColor(ChatFormatting.GREEN)))
                                            .append(Component.literal(cfg.vDisconnectMsg2.replaceAll("\\{botname}", Discordant.botName)).withStyle(ChatFormatting.WHITE)));
            }
        }
    }
}
