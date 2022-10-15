package com.kwvanderlinde.discordant.core;

import com.kwvanderlinde.discordant.core.config.DiscordantConfig;
import com.kwvanderlinde.discordant.core.discord.api.DiscordApi;
import com.kwvanderlinde.discordant.core.linkedprofiles.LinkedProfileManager;
import com.kwvanderlinde.discordant.core.logging.DiscordantAppender;

public record ConfigDependantServices(DiscordantConfig config, DiscordApi discordApi,
                                      LinkedProfileManager linkedProfileManager,
                                      DiscordantAppender logAppender, ScopeFactory scopeFactory) {
}
