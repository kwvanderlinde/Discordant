package com.kwvanderlinde.discordant.core.linkedprofiles;

import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.UUID;

// TODO How worthwhile is this class? I don't really need lazy loading from the backing store, I can
//  load everything at once. I should not support direct edits of the config during run time,
//  instead assuming that at any point I can commit the current state to disk with an overwrite
//  without worrying about losing data.
public class WriteThroughLinkedProfileRepository implements LinkedProfileRepository {
    private final @Nonnull LinkedProfileRepository front;
    private final @Nonnull LinkedProfileRepository back;

    public WriteThroughLinkedProfileRepository(@Nonnull LinkedProfileRepository front, @Nonnull LinkedProfileRepository back) {
        this.front = front;
        this.back = back;
    }

    @Override
    public Collection<LinkedProfile> getLinkedProfiles() {
        return back.getLinkedProfiles();
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
