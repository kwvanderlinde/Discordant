package com.kwvanderlinde.discordant.core.modinterfaces;

import java.nio.file.Path;

public interface Integration {
    Path getConfigRoot();

    void setLinkingCommandsEnabled(boolean linkingEnabled);

    void addHandler(ServerEventHandler handler);

    void addHandler(PlayerEventHandler handler);

    void addHandler(CommandEventHandler handler);
}
