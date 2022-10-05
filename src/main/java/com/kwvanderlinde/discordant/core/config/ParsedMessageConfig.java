package com.kwvanderlinde.discordant.core.config;

import com.kwvanderlinde.discordant.core.messages.MessageTemplate;
import com.kwvanderlinde.discordant.core.messages.scopes.NilScope;
import com.kwvanderlinde.discordant.core.messages.scopes.NotificationStateScope;
import com.kwvanderlinde.discordant.core.messages.scopes.PendingVerificationScope;
import com.kwvanderlinde.discordant.core.messages.scopes.PlayerScope;
import com.kwvanderlinde.discordant.core.messages.scopes.ServerScope;

public class ParsedMessageConfig {
    public final MessageTemplate<ServerScope> startupMsg;
    public final MessageTemplate<ServerScope> serverStopMsg;

    public final MessageTemplate<PlayerScope> joinMessage;
    public final MessageTemplate<PlayerScope> disconnectMessage;

    public final MessageTemplate<PendingVerificationScope> verificationDisconnect;
    public final MessageTemplate<PendingVerificationScope> commandLinkMsg;
    public final MessageTemplate<PlayerScope> successfulVerificationMsg;
    public final MessageTemplate<PlayerScope> alreadyLinked;
    public final MessageTemplate<PlayerScope> successLinkDiscordMsg;

    public final MessageTemplate<NilScope> codeUnlinkMsg;
    public final MessageTemplate<NilScope> codeUnlinkFail;
    public final MessageTemplate<NotificationStateScope> mentionState;

    public ParsedMessageConfig(DiscordConfig config) {
        this.startupMsg = ServerScope.parse(config.startupMsg);
        this.serverStopMsg = ServerScope.parse(config.serverStopMsg);

        this.joinMessage = PlayerScope.parse(config.joinMessage);
        this.disconnectMessage = PlayerScope.parse(config.disconnectMessage);

        this.verificationDisconnect = PendingVerificationScope.parse(config.verificationDisconnect);
        this.commandLinkMsg = PendingVerificationScope.parse(config.commandLinkMsg);
        this.successfulVerificationMsg = PlayerScope.parse(config.successfulVerificationMsg);
        this.alreadyLinked = PlayerScope.parse(config.alreadyLinked);
        this.successLinkDiscordMsg = PlayerScope.parse(config.successLinkDiscordMsg);

        this.codeUnlinkMsg = NilScope.parse(config.codeUnlinkMsg);
        this.codeUnlinkFail = NilScope.parse(config.codeUnlinkFail);
        this.mentionState = NotificationStateScope.parse(config.mentionState);
    }
}
