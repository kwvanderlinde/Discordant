package com.kwvanderlinde.discordant.core.messages.scopes;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;
import com.kwvanderlinde.discordant.core.modinterfaces.Player;
import com.kwvanderlinde.discordant.core.modinterfaces.Server;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record ServerScope(Server server, String name) implements Scope<ServerScope> {
    public static List<String> parameters() {
        return List.of("server.name", "server.motd", "server.playerCount", "server.maxPlayerCount", "server.players");
    }

    @Override
    public Map<String, SemanticMessage.Part> values() {
        return Map.of(
                "server.name", SemanticMessage.literal(name),
                "server.motd", SemanticMessage.literal(server.motd()),
                "server.playerCount", SemanticMessage.literal(String.valueOf(server.getPlayerCount())),
                "server.maxPlayerCount", SemanticMessage.literal(String.valueOf(server.getMaxPlayers())),
                "server.players", SemanticMessage.literal(server.getAllPlayers().map(Player::name).collect(Collectors.joining(", ")))
        );
    }
}
