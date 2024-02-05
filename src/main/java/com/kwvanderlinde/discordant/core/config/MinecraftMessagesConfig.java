package com.kwvanderlinde.discordant.core.config;

import com.kwvanderlinde.discordant.core.messages.scopes.DiscordUserScope;
import com.kwvanderlinde.discordant.core.messages.scopes.NilScope;
import com.kwvanderlinde.discordant.core.messages.scopes.NotificationStateScope;
import com.kwvanderlinde.discordant.core.messages.scopes.PendingVerificationScope;
import com.kwvanderlinde.discordant.core.messages.scopes.PlayerScope;

public class MinecraftMessagesConfig {
    public MinecraftMessageConfig<PendingVerificationScope> verificationDisconnect = new MinecraftMessageConfig<>("You need to verify your account via discord. Your code is {verification.code}. Send this code to {server.botName} PM.");
    public MinecraftMessageConfig<PendingVerificationScope> commandLinkMsg = new MinecraftMessageConfig<>("Your code is {verification.code}. Send this code to {server.botName} PM.");
    public MinecraftMessageConfig<PlayerScope> accountAlreadyLinked = new MinecraftMessageConfig<>("Your account is already linked to {player.discordTag}");
    public MinecraftMessageConfig<PlayerScope> adminLinkSuccessful = new MinecraftMessageConfig<>("Successfully linked the account {player.name} to {player.discordTag}!");
    public MinecraftMessageConfig<DiscordUserScope> adminLinkUnknownDiscordUser = new MinecraftMessageConfig<>("Could not find a discord user with ID {player.discordId}");
    public MinecraftMessageConfig<NilScope> codeUnlinkMsg = new MinecraftMessageConfig<>("Unlinked discord profile successfully");
    public MinecraftMessageConfig<NilScope> codeUnlinkFail = new MinecraftMessageConfig<>("Failed to unlink discord profile, profile not found!");
    public MinecraftMessageConfig<NotificationStateScope> mentionStateUpdateResponse = new MinecraftMessageConfig<>("Mention notifications are now {notifications.enablement}");
    public MinecraftMessageConfig<NotificationStateScope> mentionStateQueryResponse = new MinecraftMessageConfig<>("Mention notifications are {notifications.enablement}");
}
