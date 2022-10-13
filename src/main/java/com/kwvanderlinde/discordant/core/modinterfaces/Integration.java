package com.kwvanderlinde.discordant.core.modinterfaces;

import java.nio.file.Path;

public interface Integration {
    Path getConfigRoot();

    // TODO These should probably actually be passing callback for when the events happen.
    void enableCommands(boolean linkingEnabled);

    Events events();

    CommandHandlers commandsHandlers();
}
