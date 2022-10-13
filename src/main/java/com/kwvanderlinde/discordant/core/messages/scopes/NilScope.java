package com.kwvanderlinde.discordant.core.messages.scopes;

import com.google.common.collect.ImmutableMap;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import javax.annotation.Nonnull;

public record NilScope() implements Scope {
    @Override
    public void addValuesTo(@Nonnull ImmutableMap.Builder<String, SemanticMessage.Part> builder) {
        // This page intentionally left blank.
    }
}
