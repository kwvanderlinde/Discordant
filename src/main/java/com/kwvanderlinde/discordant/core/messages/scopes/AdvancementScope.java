package com.kwvanderlinde.discordant.core.messages.scopes;

import com.google.common.collect.ImmutableMap;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import javax.annotation.Nonnull;

public record AdvancementScope(PlayerScope base, String name, String title, String description) implements Scope {
    @Override
    public void addValuesTo(@Nonnull ImmutableMap.Builder<String, SemanticMessage.Part> builder) {
        base.addValuesTo(builder);

        builder.put("advancement.name", SemanticMessage.literal(name));
        builder.put("advancement.title", SemanticMessage.literal(title));
        builder.put("advancement.description", SemanticMessage.literal(description));
    }
}
