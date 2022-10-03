package com.kwvanderlinde.discordant.core.modinterfaces;

import java.nio.file.Path;

public interface Integration {
    Path getConfigRoot();

    Server getServer();

    // TODO These should probably actually be passing callback for when the events happen.
    void enableBaseCommands();
    void enableLinkingCommands();

    Events events();
}
