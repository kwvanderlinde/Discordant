package com.kwvanderlinde.discordant.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private final Path configRoot;
    private final Path mainConfigPath;
    private final Gson gson;

    public ConfigManager(Path configRoot) {
        this.configRoot = configRoot;
        this.mainConfigPath = configRoot.resolve("config.json");
        this.gson = new GsonBuilder().setPrettyPrinting()
                                     .serializeNulls()
                                     .registerTypeAdapter(Color.class, new ColorAdapter())
                                     .create();
    }

    public Path getConfigRoot() {
        return configRoot;
    }

    public void ensureConfigStructure() throws IOException {
        if (!Files.exists(mainConfigPath)) {
            try {
                String config = gson.toJson(new DiscordantConfig());

                File file = mainConfigPath.toFile();
                fileWriter(file, config);
            }
            catch (Throwable t) {
                t.printStackTrace(System.out);
                throw t;
            }
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

    public DiscordantConfig readDiscordLinkSettings() throws FileNotFoundException {
        return gson.fromJson(new FileReader(mainConfigPath.toFile()), DiscordantConfig.class);
    }

    private static class ColorAdapter implements JsonSerializer<Color>, JsonDeserializer<Color> {
        @Override
        public JsonElement serialize(Color src, Type typeOfSrc, JsonSerializationContext context) {
            final var rgbNoAlpha = 0x00FFFFFF & src.getRGB();
            final var colorString = String.format(
                    "%06X", rgbNoAlpha
            );
            final var result = new JsonPrimitive(colorString);
            return result;
        }

        @Override
        public Color deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            final var string = json.getAsString();
            final var result = new Color(Integer.parseInt(string, 16));
            return result;
        }
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
