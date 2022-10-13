package com.kwvanderlinde.discordant.core.messages.scopes;

import com.google.common.collect.ImmutableMap;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;
import org.jetbrains.annotations.NotNull;

public record DeathScope(PlayerScope playerScope, String message) implements Scope {
    @Override
    public void addValuesTo(@NotNull ImmutableMap.Builder<String, SemanticMessage.Part> builder) {
        playerScope.addValuesTo(builder);

        builder.put("death.message", SemanticMessage.literal(message));
    }
}
