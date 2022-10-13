package com.kwvanderlinde.discordant.core.messages.scopes;

import com.google.common.collect.ImmutableMap;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;
import org.jetbrains.annotations.NotNull;

public record NilScope() implements Scope {
    @Override
    public void addValuesTo(@NotNull ImmutableMap.Builder<String, SemanticMessage.Part> builder) {
        // This page intentionally left blank.
    }
}
