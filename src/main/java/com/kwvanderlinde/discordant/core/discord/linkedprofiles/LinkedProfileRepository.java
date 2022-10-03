package com.kwvanderlinde.discordant.core.discord.linkedprofiles;

import javax.annotation.Nullable;
import java.util.UUID;

public interface LinkedProfileRepository {
    // TODO Exceptions for failure states.

    @Nullable LinkedProfile get(UUID uuid);

    void put(LinkedProfile profile);

    void delete(UUID uuid);
}
