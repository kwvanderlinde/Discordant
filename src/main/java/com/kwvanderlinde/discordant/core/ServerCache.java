package com.kwvanderlinde.discordant.core;

import javax.annotation.Nullable;

/**
 * A place to store data that should be persisted across restarts.
 */
public interface ServerCache {
    @Nullable String get(String key);

    void put(String key, String value);

    void delete(String key);
}
