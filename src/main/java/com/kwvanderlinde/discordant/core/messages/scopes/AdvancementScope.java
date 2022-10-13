package com.kwvanderlinde.discordant.core.messages.scopes;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import javax.annotation.Nonnull;
import java.util.Map;

public record AdvancementScope(PlayerScope base, String name, String title, String description) implements SingleDerivedScope<PlayerScope> {
    @Override
    public @Nonnull Map<String, SemanticMessage.Part> notInheritedValues() {
        return Map.of(
                "advancement.name", SemanticMessage.literal(name),
                "advancement.title", SemanticMessage.literal(title),
                "advancement.description", SemanticMessage.literal(description)
        );
    }
}
