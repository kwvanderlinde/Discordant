package com.kwvanderlinde.discordant.core;

import com.kwvanderlinde.discordant.core.discord.api.DiscordApi;
import com.kwvanderlinde.discordant.core.linkedprofiles.LinkedProfileManager;

public record ConfigDependantServices(DiscordApi discordApi,
                                      LinkedProfileManager linkedProfileManager,
                                      ScopeFactory scopeFactory) {
}
