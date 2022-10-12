package com.kwvanderlinde.discordant.core.messages.scopes;

import com.google.common.collect.ImmutableMap;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import javax.annotation.Nonnull;
import java.util.Map;

public interface DerivedScope<Base extends Scope> extends Scope {
    @Nonnull Base base();

    @Nonnull Map<String, SemanticMessage.Part> notInheritedValues();

    @Override
    default @Nonnull Map<String, SemanticMessage.Part> values() {
        final var baseValues = base().values();
        final var thisValues = notInheritedValues();

        return ImmutableMap.<String, SemanticMessage.Part> builder()
                           .putAll(baseValues)
                           .putAll(thisValues).build();
    }
}
