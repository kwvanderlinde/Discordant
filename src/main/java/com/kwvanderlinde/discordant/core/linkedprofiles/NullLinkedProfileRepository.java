package com.kwvanderlinde.discordant.core.linkedprofiles;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class NullLinkedProfileRepository implements LinkedProfileRepository {
    @Override
    public Collection<LinkedProfile> getLinkedProfiles() {
        return Collections.emptyList();
    }

    @Override
    public @Nullable LinkedProfile getByPlayerId(UUID uuid) {
        return null;
    }

    @Override
    public void put(LinkedProfile profile) {
    }

    @Override
    public void delete(LinkedProfile profile) {
    }
}
