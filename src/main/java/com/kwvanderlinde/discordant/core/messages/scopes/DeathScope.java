package com.kwvanderlinde.discordant.core.messages.scopes;

import com.google.common.collect.ImmutableMap;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import javax.annotation.Nonnull;

public record DeathScope(PlayerScope playerScope, String message) implements Scope {
    @Override
    public void addValuesTo(@Nonnull ImmutableMap.Builder<String, SemanticMessage.Part> builder) {
        playerScope.addValuesTo(builder);

        builder.put("death.message", SemanticMessage.literal(message));
    }
}
