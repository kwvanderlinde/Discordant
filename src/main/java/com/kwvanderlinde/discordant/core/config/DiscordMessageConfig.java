package com.kwvanderlinde.discordant.core.config;

import com.kwvanderlinde.discordant.core.messages.ScopeUtil;
import com.kwvanderlinde.discordant.core.messages.PlainTextRenderer;
import com.kwvanderlinde.discordant.core.messages.scopes.NilScope;
import com.kwvanderlinde.discordant.core.messages.scopes.Scope;

import javax.annotation.Nullable;
import java.awt.Color;

public class DiscordMessageConfig<T extends Scope<T>> {
    public @Nullable String title;
    public @Nullable Color color;
    public @Nullable String image;
    public @Nullable String thumbnail;
    public @Nullable String description;

    // TODO Add a mechanism for parsing or validating the message eagerly on server start rather
    //  than waiting for it to be used.

    public DiscordMessageConfig() {
        this(null, null, null, null, null);
    }

    public DiscordMessageConfig(@Nullable String title, @Nullable Color color, @Nullable String image, @Nullable String thumbnail, @Nullable String description) {
        this.title = title;
        this.color = color;
        this.image = image;
        this.thumbnail = thumbnail;
        this.description = description;
    }

    public DiscordMessageConfig<NilScope> instantiate(T scope) {
        final var result = new DiscordMessageConfig<NilScope>();
        final var scopeValues = scope.values();

        if (title != null) {
            result.title = ScopeUtil.instantiate(title, scopeValues)
                                    .reduce(PlainTextRenderer.instance());
        }
        result.color = color;
        if (image != null) {
            result.image = ScopeUtil.instantiate(image, scopeValues)
                                        .reduce(PlainTextRenderer.instance());
        }
        if (thumbnail != null) {
            result.thumbnail = ScopeUtil.instantiate(thumbnail, scopeValues)
                                        .reduce(PlainTextRenderer.instance());
        }
        if (description != null) {
            result.description = ScopeUtil.instantiate(description, scopeValues)
                                          .reduce(PlainTextRenderer.instance());
        }

        return result;
    }
}
