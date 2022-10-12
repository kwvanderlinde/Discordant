package com.kwvanderlinde.discordant.core.modinterfaces;

import java.util.UUID;

/**
 * Describes a user account.
 */
public interface Profile {
    UUID uuid();

    String name();
}
