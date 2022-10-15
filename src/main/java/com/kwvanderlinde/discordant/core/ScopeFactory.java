package com.kwvanderlinde.discordant.core;

import com.kwvanderlinde.discordant.core.messages.scopes.ServerScope;
import com.kwvanderlinde.discordant.core.modinterfaces.Player;
import com.kwvanderlinde.discordant.core.modinterfaces.Server;

import javax.annotation.Nonnull;

public class ScopeFactory {
    private final Discordant discordant;

    public ScopeFactory(Discordant discordant) {
        this.discordant = discordant;
    }

    public ServerScope serverScope(@Nonnull Server server) {
        return new ServerScope(
                discordant.getConfig().minecraft.serverName,
                discordant.getBotName(),
                server.motd(),
                server.getPlayerCount(),
                server.getMaxPlayers(),
                server.getAllPlayers().map(Player::name).toList(),
                discordant.getCurrentTime()
        );
    }
}
