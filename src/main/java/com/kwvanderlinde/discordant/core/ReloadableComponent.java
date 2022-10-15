package com.kwvanderlinde.discordant.core;

import com.kwvanderlinde.discordant.core.config.DiscordantConfig;

public interface ReloadableComponent {
    // TODO Return future to indicate completion.
    void reload(DiscordantConfig newConfig);
}
