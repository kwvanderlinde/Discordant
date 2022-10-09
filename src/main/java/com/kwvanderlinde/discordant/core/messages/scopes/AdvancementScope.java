package com.kwvanderlinde.discordant.core.messages.scopes;

import com.google.common.collect.ImmutableMap;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;
import com.kwvanderlinde.discordant.core.modinterfaces.Advancement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public record AdvancementScope(PlayerScope playerScope, Advancement advancement) implements Scope<AdvancementScope> {
    public static List<String> parameters() {
        final var parameters = new ArrayList<String>();
        parameters.addAll(List.of("advancement.name", "advancement.title", "advancement.description"));
        parameters.addAll(PlayerScope.parameters());
        return Collections.unmodifiableList(parameters);
    }

    @Override
    public Map<String, SemanticMessage.Part> values() {
        final var playerValues = playerScope.values();
        final var advancementValues = Map.of(
                "advancement.name", SemanticMessage.literal(advancement.name()),
                "advancement.title", SemanticMessage.literal(advancement.title()),
                "advancement.description", SemanticMessage.literal(advancement.description())
        );

        return ImmutableMap.<String, SemanticMessage.Part> builder()
                           .putAll(playerValues)
                           .putAll(advancementValues).build();
    }
}
