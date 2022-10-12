package com.kwvanderlinde.discordant.core.config;

import com.kwvanderlinde.discordant.core.messages.ScopeUtil;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;
import com.kwvanderlinde.discordant.core.messages.scopes.Scope;

import javax.annotation.Nullable;

public class MinecraftMessageConfig<T extends Scope> {
    public @Nullable String text;

    // TODO Add a mechanism for parsing or validating the message eagerly on server start rather
    //  than waiting for it to be used.

    public MinecraftMessageConfig(@Nullable String text) {
        this.text = text;
    }

    public SemanticMessage instantiate(T scope) {
        final var scopeValues = scope.values();
        return ScopeUtil.instantiate(text, scopeValues);
    }
}
