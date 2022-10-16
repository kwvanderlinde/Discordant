package com.kwvanderlinde.discordant.core;

import com.kwvanderlinde.discordant.core.discord.api.ReplaceableDiscordApi;
import com.kwvanderlinde.discordant.core.linkedprofiles.LinkedProfileManager;

public record ConfigDependantServices(ReplaceableDiscordApi discordApi,
                                      LinkedProfileManager linkedProfileManager,
                                      ScopeFactory scopeFactory) {
}
