package com.kwvanderlinde.discordant.core.linkedprofiles;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;

/**
 * Provides access to all known linked profiles.
 */
public interface LinkedProfileRepository {
    // TODO Exceptions for failure states.

    Collection<LinkedProfile> getLinkedProfiles();

    default @Nullable LinkedProfile getByPlayerName(String name) {
        return getLinkedProfiles().stream().filter(profile -> profile.playerName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    /**
     * Looks up a linked profile by UUID.
     *
     * @param uuid The Minecraft player UUID.
     * @return The linked profile associated with the uuid, or null if there is none.
     */
    @Nullable LinkedProfile getByPlayerId(UUID uuid);

    /**
     * Adds or updates a linked profile.
     *
     * @param profile A linked profile to upsert.
     */
    void put(LinkedProfile profile);

    /**
     * Removes a linked profile.
     *
     * @param profile The profile to delete.
     */
    void delete(LinkedProfile profile);
}
