package com.kwvanderlinde.discordant.core;

import com.kwvanderlinde.discordant.core.config.DiscordantConfig;
import com.kwvanderlinde.discordant.core.discord.api.DiscordApi;
import com.kwvanderlinde.discordant.core.linkedprofiles.LinkedProfileManager;
import com.kwvanderlinde.discordant.core.logging.DiscordantAppender;

public class ConfigDependantServices {
    public final DiscordantConfig config;
    public final DiscordApi discordApi;
    public final LinkedProfileManager linkedProfileManager;
    public final DiscordantAppender logAppender;
    public final ScopeFactory scopeFactory;

    public ConfigDependantServices(DiscordantConfig config,
                                   DiscordApi discordApi,
                                   LinkedProfileManager linkedProfileManager,
                                   DiscordantAppender logAppender,
                                   ScopeFactory scopeFactory) {
        this.config = config;
        this.discordApi = discordApi;
        this.linkedProfileManager = linkedProfileManager;
        this.logAppender = logAppender;
        this.scopeFactory = scopeFactory;
    }
}
