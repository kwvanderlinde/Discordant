package com.kwvanderlinde.discordant.core.discord.linkedprofiles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class ConfigProfileRepository implements LinkedProfileRepository {
    private final Path profileDirectory;

    public ConfigProfileRepository(Path profileDirectory) {
        this.profileDirectory = profileDirectory;
    }

    private Path getProfilePath(UUID uuid) {
        return this.profileDirectory.resolve(uuid.toString() + ".json");
    }

    @Override
    public @Nullable LinkedProfile getByPlayerId(UUID uuid) {
        final var path = getProfilePath(uuid);
        if (Files.exists(path)) {
            try (final var reader = new FileReader(path.toFile())) {
                return new Gson().fromJson(reader, LinkedProfile.class);
            }
            catch (IOException e) {
                return null;
            }
        }
        else {
            return null;
        }
    }

    @Override
    public void put(LinkedProfile profile) {
        final var path = getProfilePath(profile.uuid());
        final var gson = new GsonBuilder().setPrettyPrinting().create().toJson(profile);
        final var file = path.toFile();

        try {
            Files.createDirectories(profileDirectory);
            final var created = file.createNewFile();
            if (!created) {
                // TODO Report failure.
            }
        }
        catch (IOException e) {
            // TODO Don't just print a stack trace. Log it!
            e.printStackTrace();
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(gson);
        }
        catch (IOException e) {
            // TODO Don't just print a stack trace. Log it!
            e.printStackTrace();
        }
    }

    @Override
    public void delete(LinkedProfile profile) {
        final var path = getProfilePath(profile.uuid());
        final var deleted = path.toFile().delete();
        if (!deleted) {
            // TODO Report failure so upstream code can notify that user if needed.
        }
    }
}
