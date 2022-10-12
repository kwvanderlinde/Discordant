package com.kwvanderlinde.discordant.core.messages.scopes;

import com.google.common.collect.ImmutableMap;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record ServerScope(String name, String botName, String motd, int playerCount, int maxPlayerCount, List<String> playerNames, long time) implements Scope {
    @Override
    public void addValuesTo(@NotNull ImmutableMap.Builder<String, SemanticMessage.Part> builder) {
        builder.put("server.name", SemanticMessage.literal(name));
        builder.put("server.botName", SemanticMessage.literal(botName));
        builder.put("server.motd", SemanticMessage.literal(motd));
        builder.put("server.playerCount", SemanticMessage.literal(String.valueOf(playerCount)));
        builder.put("server.maxPlayerCount", SemanticMessage.literal(String.valueOf(maxPlayerCount)));
        builder.put("server.players", SemanticMessage.literal(String.join(", ", playerNames)));
        builder.put("server.time", SemanticMessage.literal(String.valueOf(time)));
    }
}
