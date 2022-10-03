package com.kwvanderlinde.discordant.core.discord;

import javax.annotation.Nullable;

public interface LinkedProfileRepository {
    // TODO Exceptions for failure states.

    @Nullable LinkedProfile get(String uuid);

    void put(LinkedProfile profile);

    void delete(String uuid);
}
