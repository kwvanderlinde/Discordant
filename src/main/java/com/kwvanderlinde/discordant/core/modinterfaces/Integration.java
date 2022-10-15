package com.kwvanderlinde.discordant.core.modinterfaces;

import java.nio.file.Path;

public interface Integration {
    Path getConfigRoot();

    void setLinkingCommandsEnabled(boolean linkingEnabled);

    Events events();

    CommandHandlers commandsHandlers();
}
