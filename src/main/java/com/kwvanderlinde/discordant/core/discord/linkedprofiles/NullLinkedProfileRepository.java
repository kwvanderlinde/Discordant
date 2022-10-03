package com.kwvanderlinde.discordant.core.discord.linkedprofiles;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class NullLinkedProfileRepository implements LinkedProfileRepository {
    @Override
    public @Nullable LinkedProfile get(UUID uuid) {
        return null;
    }

    @Override
    public void put(LinkedProfile profile) {
    }

    @Override
    public void delete(UUID uuid) {
    }
}
