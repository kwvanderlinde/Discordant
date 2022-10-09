package com.kwvanderlinde.discordant.core.messages.scopes;

import com.google.common.collect.ImmutableMap;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public record ChatScope(PlayerScope playerScope, String message) implements Scope<ChatScope> {
    public static List<String> parameters() {
        final var parameters = new ArrayList<String>();
        parameters.addAll(List.of("chat.message"));
        parameters.addAll(PlayerScope.parameters());
        return Collections.unmodifiableList(parameters);
    }

    @Override
    public Map<String, SemanticMessage.Part> values() {
        final var playerValues = playerScope.values();
        final var chatValues = Map.of(
                "chat.message", SemanticMessage.literal(message)
        );

        return ImmutableMap.<String, SemanticMessage.Part> builder()
                           .putAll(playerValues)
                           .putAll(chatValues).build();
    }
}
