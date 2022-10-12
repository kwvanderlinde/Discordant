package com.kwvanderlinde.discordant.core.messages.scopes;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import javax.annotation.Nonnull;
import java.util.Map;

public record NilScope() implements Scope {
    @Override
    public @Nonnull Map<String, SemanticMessage.Part> values() {
        return Map.of();
    }
}
