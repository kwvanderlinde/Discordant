package com.kwvanderlinde.discordant.core;

import com.kwvanderlinde.discordant.core.messages.PlainTextRenderer;
import com.kwvanderlinde.discordant.core.messages.ScopeUtil;
import com.kwvanderlinde.discordant.core.messages.scopes.DiscordUserScope;
import com.kwvanderlinde.discordant.core.messages.scopes.PlayerScope;
import com.kwvanderlinde.discordant.core.messages.scopes.ProfileScope;
import com.kwvanderlinde.discordant.core.messages.scopes.ServerScope;
import com.kwvanderlinde.discordant.core.modinterfaces.Player;
import com.kwvanderlinde.discordant.core.modinterfaces.Profile;
import com.kwvanderlinde.discordant.core.modinterfaces.Server;
import net.dv8tion.jda.api.entities.User;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

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

    public ProfileScope profileScope(Profile profile, Server server) {
        return new ProfileScope(
                serverScope(server),
                profile.uuid(),
                profile.name()
        );
    }

    public PlayerScope playerScope(Profile profile, Server server, User user) {
        return new PlayerScope(
                profileScope(profile, server),
                (user == null)
                        ? new DiscordUserScope("", "", "")
                        : new DiscordUserScope(user.getId(), user.getName(), user.getAsTag()),
                getPlayerIconUrl(profile, server),
                getAvatarUrls(profile, server));
    }

    public String getPlayerIconUrl(Profile profile, Server server) {
        final var scopeValues = profileScope(profile, server).values();
        final var config = discordant.getConfig();
        return ScopeUtil.instantiate(config.playerIconUrl, scopeValues)
                        .reduce(PlainTextRenderer.instance());
    }

    public Map<String, String> getAvatarUrls(Profile profile, Server server) {
        final var result = new HashMap<String, String>();
        final var config = discordant.getConfig();
        final var scopeValues = profileScope(profile, server).values();
        for (final var entry : config.avatarUrls.entrySet()) {
            final var url = ScopeUtil.instantiate(entry.getValue(), scopeValues)
                                     .reduce(PlainTextRenderer.instance());
            result.put(entry.getKey(), url);
        }
        return result;
    }
}
