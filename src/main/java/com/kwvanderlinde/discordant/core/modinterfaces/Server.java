package com.kwvanderlinde.discordant.core.modinterfaces;

import java.util.UUID;
import java.util.stream.Stream;

public interface Server {
    int getTickCount();

    int getPlayerCount();

    int getMaxPlayers();

    Player getPlayer(UUID uuid);

    Stream<Player> getAllPlayers();

    String motd();
}
