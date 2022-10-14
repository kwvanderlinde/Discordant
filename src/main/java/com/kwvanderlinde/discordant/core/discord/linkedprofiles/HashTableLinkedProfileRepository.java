package com.kwvanderlinde.discordant.core.discord.linkedprofiles;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of LinkedProfileRepository where the profile are stored in memory.
 */
public class HashTableLinkedProfileRepository implements LinkedProfileRepository {
    private final Map<UUID, LinkedProfile> profiles = new HashMap<>();

    @Override
    public @Nullable LinkedProfile getByPlayerId(UUID uuid) {
        return profiles.get(uuid);
    }

    @Override
    public void put(LinkedProfile profile) {
        profiles.put(profile.uuid(), profile);
    }

    @Override
    public void delete(LinkedProfile profile) {
        profiles.remove(profile.uuid());
    }
}
