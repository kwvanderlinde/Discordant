package com.kwvanderlinde.discordant.core;

import com.kwvanderlinde.discordant.core.config.DiscordantConfig;
import com.kwvanderlinde.discordant.core.messages.PlainTextRenderer;
import com.kwvanderlinde.discordant.core.messages.ScopeUtil;
import com.kwvanderlinde.discordant.core.messages.scopes.DiscordUserScope;
import com.kwvanderlinde.discordant.core.messages.scopes.PlayerScope;
import com.kwvanderlinde.discordant.core.messages.scopes.ProfileScope;
import com.kwvanderlinde.discordant.core.messages.scopes.ServerScope;
import com.kwvanderlinde.discordant.core.modinterfaces.Player;
import com.kwvanderlinde.discordant.core.modinterfaces.Profile;
import com.kwvanderlinde.discordant.core.modinterfaces.Server;
import com.kwvanderlinde.discordant.core.utils.Clock;
import net.dv8tion.jda.api.entities.User;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class ScopeFactory {
    private final Clock clock;
    private final DiscordantConfig config;
    private final String botName;

    public ScopeFactory(Clock clock, DiscordantConfig config, String botName) {
        this.clock = clock;
        this.config = config;
        this.botName = botName;
    }

    public ServerScope serverScope(@Nonnull Server server) {
        return new ServerScope(
                config.minecraft.serverName,
                botName,
                server.motd(),
                server.getPlayerCount(),
                server.getMaxPlayers(),
                server.getAllPlayers().map(Player::name).toList(),
                clock.getCurrentTime()
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
        return ScopeUtil.instantiate(config.playerIconUrl, scopeValues)
                        .reduce(PlainTextRenderer.instance());
    }

    public Map<String, String> getAvatarUrls(Profile profile, Server server) {
        final var result = new HashMap<String, String>();
        final var scopeValues = profileScope(profile, server).values();
        for (final var entry : config.avatarUrls.entrySet()) {
            final var url = ScopeUtil.instantiate(entry.getValue(), scopeValues)
                                     .reduce(PlainTextRenderer.instance());
            result.put(entry.getKey(), url);
        }
        return result;
    }
}
