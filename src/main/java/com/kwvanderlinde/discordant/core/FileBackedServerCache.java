package com.kwvanderlinde.discordant.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileBackedServerCache implements ServerCache {
    private static final Logger logger = LogManager.getLogger(FileBackedServerCache.class);

    private final Path cacheRoot;

    public FileBackedServerCache(Path cacheRoot) {
        this.cacheRoot = cacheRoot;
    }

    @Override
    public @Nullable String get(String key) {
        try {
            return Files.readString(makePath(key));
        }
        catch (IOException e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public void put(String key, String value) {
        try {
            Files.createDirectories(cacheRoot);
            Files.writeString(makePath(key), value, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        }
        catch (IOException e) {
            logger.error(e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.delete(makePath(key));
        }
        catch (IOException e) {
            logger.error(e);
        }
    }

    private Path makePath(String key) {
        // TODO Could be made more robust, surely.
        return cacheRoot.resolve(key);
    }
}
