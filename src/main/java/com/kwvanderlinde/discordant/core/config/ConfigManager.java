package com.kwvanderlinde.discordant.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private final Path configRoot;
    private final Path mainConfigPath;

    public ConfigManager(Path configRoot) {
        this.configRoot = configRoot;
        this.mainConfigPath = configRoot.resolve("config.json");
    }

    public Path getConfigRoot() {
        return configRoot;
    }

    public void ensureConfigStructure() throws IOException {
        if (!Files.exists(mainConfigPath)) {
            String gson = new GsonBuilder().setPrettyPrinting().create().toJson(new DiscordConfig());

            File file = mainConfigPath.toFile();
            fileWriter(file, gson);
        }

        final var linkedProfiles = configRoot.resolve("linked-profiles");
        // Why do we need version-specific languages? What do these languages even do?
        final var languages = configRoot.resolve("languages");
        final var languages119 = languages.resolve("1.19");

        Files.createDirectories(configRoot);
        Files.createDirectories(linkedProfiles);
        Files.createDirectories(languages);
        Files.createDirectories(languages119);
    }

    public DiscordConfig readDiscordLinkSettings() throws FileNotFoundException {
        return new Gson().fromJson(new FileReader(mainConfigPath.toFile()), DiscordConfig.class);
    }

    private static void fileWriter(File file, String gson) {
        try {
            file.createNewFile();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(gson);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
