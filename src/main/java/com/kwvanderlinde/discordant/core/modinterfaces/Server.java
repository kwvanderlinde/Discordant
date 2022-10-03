package com.kwvanderlinde.discordant.core.modinterfaces;

import java.util.UUID;

public interface Server {
    int getTickCount();

    int getPlayerCount();

    int getMaxPlayers();

    Player getPlayer(UUID uuid);
}
