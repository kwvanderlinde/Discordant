package com.kwvanderlinde.discordant.core.messages.scopes;

import com.google.common.collect.ImmutableMap;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public record DeathScope(PlayerScope playerScope, String message) implements Scope<DeathScope> {
    public static List<String> parameters() {
        final var parameters = new ArrayList<String>();
        parameters.addAll(List.of("death.message"));
        parameters.addAll(PlayerScope.parameters());
        return Collections.unmodifiableList(parameters);
    }

    @Override
    public Map<String, SemanticMessage.Part> values() {
        final var playerValues = playerScope.values();
        final var deathValues = Map.of(
                "death.message", SemanticMessage.literal(message)
        );

        return ImmutableMap.<String, SemanticMessage.Part> builder()
                           .putAll(playerValues)
                           .putAll(deathValues).build();
    }
}
