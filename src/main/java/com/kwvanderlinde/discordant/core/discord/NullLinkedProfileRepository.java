package com.kwvanderlinde.discordant.core.discord;

import org.jetbrains.annotations.Nullable;

public class NullLinkedProfileRepository implements LinkedProfileRepository {
    @Override
    public @Nullable LinkedProfile get(String uuid) {
        return null;
    }

    @Override
    public void put(LinkedProfile profile) {
    }

    @Override
    public void delete(String uuid) {
    }
}
