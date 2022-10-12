package com.kwvanderlinde.discordant.core.messages.scopes;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import javax.annotation.Nonnull;
import java.util.Map;

public record ChatScope(PlayerScope playerScope, String message) implements DerivedScope<PlayerScope> {
    @Override
    public @Nonnull PlayerScope base() {
        return playerScope;
    }

    @Override
    public @Nonnull Map<String, SemanticMessage.Part> notInheritedValues() {
        return Map.of(
                "chat.message", SemanticMessage.literal(message)
        );
    }
}
