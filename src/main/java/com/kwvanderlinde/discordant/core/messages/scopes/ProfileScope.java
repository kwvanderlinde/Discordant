package com.kwvanderlinde.discordant.core.messages.scopes;

import com.google.common.collect.ImmutableMap;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;

public record ProfileScope(ServerScope serverScope, UUID playerUuid, String playerName)
        implements DerivedScope<ServerScope> {

    @Override
    public @Nonnull ServerScope base() {
        return serverScope;
    }

    @Override
    public @Nonnull Map<String, SemanticMessage.Part> notInheritedValues() {
        return ImmutableMap.<String, SemanticMessage.Part> builder()
                           .putAll(Map.of("player.uuid", SemanticMessage.literal(playerUuid.toString()),
                                          "player.name", SemanticMessage.literal(playerName)))
                           .build();
    }
}
