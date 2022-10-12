package com.kwvanderlinde.discordant.core.messages.scopes;

import com.google.common.collect.ImmutableMap;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import javax.annotation.Nonnull;
import java.util.Map;

public interface Scope {
    /**
     * Get all the scope's parameters as a mapping from parameter names to semantic message parts.
     *
     * The scope's parameters include those defined by the inherited scopes.
     *
     * @return All the parameters in the scope.
     */
    default Map<String, SemanticMessage.Part> values() {
        final var builder = ImmutableMap.<String, SemanticMessage.Part> builder();
        addValuesTo(builder);
        return builder.build();
    }

    /**
     * Adds all parameters for the scope to the builder.
     *
     * The scope is responsible for instructing its inherited scopes to add their values as well.
     *
     * @param builder The map builder to which parameters can be added.
     */
    void addValuesTo(@Nonnull ImmutableMap.Builder<String, SemanticMessage.Part> builder);
}