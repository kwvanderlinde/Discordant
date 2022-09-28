package com.kwvanderlinde.discordant.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kwvanderlinde.discordant.discord.DiscordConfig;
import com.kwvanderlinde.discordant.discord.Discordant;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigManager {

    public void genDiscordLinkSettings() {
        if (!Files.exists(Paths.get("./config/discordant/config.json"))) {
            String gson = new GsonBuilder().setPrettyPrinting().create().toJson(new DiscordConfig());
            File file = new File("./config/discordant/config.json");
            fileWriter(file, gson);
        }
    }

    public void writeCurrentConfigInstance() {
        String gson = new GsonBuilder().setPrettyPrinting().create().toJson(Discordant.config);
        File file = new File("./config/discordant/config.json");
        fileWriter(file, gson);
    }

    public DiscordConfig readDiscordLinkSettings() throws FileNotFoundException {
        return new Gson().fromJson(new FileReader("./config/discordant/config.json"), DiscordConfig.class);
    }


    public void craftPaths() throws IOException {
        final var top = Paths.get("./config/discordant");
        final var linkedProfiles = top.resolve("linked-profiles");
        // Why do we need version-specific languages? What do these languages even do?
        final var languages = top.resolve("languages");
        final var languages119 = languages.resolve("1.19");

        Files.createDirectories(top);
        Files.createDirectories(linkedProfiles);
        Files.createDirectories(languages);
        Files.createDirectories(languages119);
    }

    @Nullable
    public static LinkedProfile getLinkedProfile(String uuid) throws IOException {
        String path = String.format("./config/discordant/linked-profiles/%s.json", uuid);
        if (Files.exists(Paths.get(path))) {
            FileReader reader = new FileReader(path);
            LinkedProfile profile = new Gson().fromJson(reader, LinkedProfile.class);
            reader.close();
            return profile;
        }
        else {
            return null;
        }
    }

    public static void saveLinkedProfile(LinkedProfile profile) {
        String gson = new GsonBuilder().setPrettyPrinting().create().toJson(profile);
        File file = new File(String.format("./config/discordant/linked-profiles/%s.json", profile.uuid));
        fileWriter(file, gson);
    }

    public static void fileWriter(File file, String gson) {
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
