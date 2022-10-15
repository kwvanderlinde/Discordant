package com.kwvanderlinde.discordant.core.linkedprofiles;

import com.kwvanderlinde.discordant.core.linkedprofiles.api.LinkedProfile;
import com.kwvanderlinde.discordant.core.linkedprofiles.api.LinkedProfileRepository;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class NullLinkedProfileRepository implements LinkedProfileRepository {
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
