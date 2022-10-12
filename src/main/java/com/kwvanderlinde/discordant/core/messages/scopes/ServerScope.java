package com.kwvanderlinde.discordant.core.messages.scopes;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public record ServerScope(String name, String botName, String motd, int playerCount, int maxPlayerCount, List<String> playerNames, long time) implements Scope {
    @Override
    public @Nonnull Map<String, SemanticMessage.Part> values() {
        return Map.of(
                "server.name", SemanticMessage.literal(name),
                "server.botName", SemanticMessage.literal(botName),
                "server.motd", SemanticMessage.literal(motd),
                "server.playerCount", SemanticMessage.literal(String.valueOf(playerCount)),
                "server.maxPlayerCount", SemanticMessage.literal(String.valueOf(maxPlayerCount)),
                "server.players", SemanticMessage.literal(String.join(", ", playerNames)),
                "server.time", SemanticMessage.literal(String.valueOf(time))
        );
    }
}
