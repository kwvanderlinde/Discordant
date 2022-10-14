package com.kwvanderlinde.discordant.core.discord.linkedprofiles;

import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.UUID;

public class WriteThroughLinkedProfileRepository implements LinkedProfileRepository {
    private final @Nonnull LinkedProfileRepository front;
    private final @Nonnull LinkedProfileRepository back;

    public WriteThroughLinkedProfileRepository(@Nonnull LinkedProfileRepository front, @Nonnull LinkedProfileRepository back) {
        this.front = front;
        this.back = back;
    }

    @Nullable
    @Override
    public LinkedProfile getByPlayerId(UUID uuid) {
        var profile = front.getByPlayerId(uuid);

        if (profile == null) {
            // Profile not known yet.
            profile = back.getByPlayerId(uuid);
            if (profile != null) {
                // Cache it.
                front.put(profile);
            }
        }

        return profile;
    }

    @Override
    public void put(LinkedProfile profile) {
        front.put(profile);
        // Write through.
        back.put(profile);
    }

    @Override
    public void delete(LinkedProfile profile) {
        front.delete(profile);
        back.delete(profile);
    }
}
