package com.kwvanderlinde.discordant.core;

import com.kwvanderlinde.discordant.core.config.DiscordMessageConfig;
import com.kwvanderlinde.discordant.core.messages.scopes.NilScope;
import net.dv8tion.jda.api.EmbedBuilder;

public class EmbedFactory {
    public EmbedBuilder embedBuilder(DiscordMessageConfig<NilScope> config) {
        final var e = new EmbedBuilder();
        if (config.title != null) {
            e.setTitle(config.title);
        }
        if (config.color != null) {
            e.setColor(config.color.getRGB());
        }
        if (config.image != null) {
            e.setImage(config.image);
        }
        if (config.thumbnail != null) {
            e.setThumbnail(config.thumbnail);
        }
        if (config.description != null) {
            e.setDescription(config.description);
        }
        return e;
    }

}
