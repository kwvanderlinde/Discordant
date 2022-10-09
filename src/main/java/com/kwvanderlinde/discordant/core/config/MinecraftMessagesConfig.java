package com.kwvanderlinde.discordant.core.config;

import com.kwvanderlinde.discordant.core.messages.scopes.NilScope;
import com.kwvanderlinde.discordant.core.messages.scopes.NotificationStateScope;
import com.kwvanderlinde.discordant.core.messages.scopes.PendingVerificationScope;

public class MinecraftMessagesConfig {
    public MinecraftMessageConfig<PendingVerificationScope> verificationDisconnect = new MinecraftMessageConfig<>("You need to verify your account via discord. Your code is {code}. Send this code to {botname} PM.");
    public MinecraftMessageConfig<PendingVerificationScope> commandLinkMsg = new MinecraftMessageConfig<>("Your code is {code}. Send this code to {botname} PM.");
    public MinecraftMessageConfig<NilScope> codeUnlinkMsg = new MinecraftMessageConfig<>("Unlinked discord profile successfully");
    public MinecraftMessageConfig<NilScope> codeUnlinkFail = new MinecraftMessageConfig<>("Failed to unlink discord profile, profile not found!");
    public MinecraftMessageConfig<NotificationStateScope> mentionState = new MinecraftMessageConfig<>("Mention sound notifications is now set to {state}");
}
