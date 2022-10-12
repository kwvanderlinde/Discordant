package com.kwvanderlinde.discordant.core.config;

import com.kwvanderlinde.discordant.core.messages.ScopeUtil;
import com.kwvanderlinde.discordant.core.messages.PlainTextRenderer;
import com.kwvanderlinde.discordant.core.messages.scopes.NilScope;
import com.kwvanderlinde.discordant.core.messages.scopes.Scope;

import javax.annotation.Nullable;

public class TopicConfig<T extends Scope> {
    public @Nullable String description;

    // TODO Add a mechanism for parsing or validating the message eagerly on server start rather
    //  than waiting for it to be used.

    public TopicConfig() {
        this(null);
    }

    public TopicConfig(@Nullable String description) {
        this.description = description;
    }

    public TopicConfig<NilScope> instantiate(T scope) {
        final var result = new TopicConfig<NilScope>();
        final var scopeValues = scope.values();

        if (description != null) {
            result.description = ScopeUtil.instantiate(description, scopeValues)
                                          .reduce(PlainTextRenderer.instance());
        }

        return result;
    }
}
