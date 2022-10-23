package com.kwvanderlinde.discordant.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private static final Logger logger = LogManager.getLogger(ConfigManager.class);

    private final Path configRoot;
    private final Path mainConfigPath;
    private final Gson gson;

    public ConfigManager(Path configRoot) {
        this.configRoot = configRoot;
        this.mainConfigPath = configRoot.resolve("config.json");
        this.gson = new GsonBuilder().setPrettyPrinting()
                                     .serializeNulls()
                                     .disableHtmlEscaping()
                                     .registerTypeAdapter(Color.class, new ColorAdapter())
                                     .registerTypeAdapter(RenderUrlConfig.class, new RenderUrlConfigAdapter())
                                     .create();
    }

    public void ensureConfigStructure() throws IOException {
        Files.createDirectories(configRoot);

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
    }

    public DiscordantConfig readConfigSettings() throws FileNotFoundException {
        return gson.fromJson(new FileReader(mainConfigPath.toFile()), DiscordantConfig.class);
    }

    private static class ColorAdapter implements JsonSerializer<Color>, JsonDeserializer<Color> {
        @Override
        public JsonElement serialize(Color src, Type typeOfSrc, JsonSerializationContext context) {
            final var rgbNoAlpha = 0x00FFFFFF & src.getRGB();
            final var colorString = String.format(
                    "%06X", rgbNoAlpha
            );
            return new JsonPrimitive(colorString);
        }

        @Override
        public Color deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new Color(Integer.parseInt(json.getAsString(), 16));
        }
    }

    private static class RenderUrlConfigAdapter implements JsonSerializer<RenderUrlConfig>, JsonDeserializer<RenderUrlConfig> {
        private static final String PLAYER_KEY = "player";

        @Override
        public JsonElement serialize(RenderUrlConfig src, Type typeOfSrc, JsonSerializationContext context) {
            final var result = new JsonObject();

            result.add(PLAYER_KEY, new JsonPrimitive(src.icon));
            for (final var entry : src.other.entrySet()) {
                result.add(entry.getKey(), new JsonPrimitive(entry.getValue()));
            }

            return result;
        }

        @Override
        public RenderUrlConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            final var result = new RenderUrlConfig();

            // Since there is a configuration, we eliminate any default extras.
            // We'll keep the player URL since that one is important to have and can be overwritten.
            result.other.clear();

            final var object = json.getAsJsonObject();
            for (final var key : object.keySet()) {
                final String url;
                try {
                    url = object.get(key).getAsString();
                }
                catch (ClassCastException e) {
                    logger.error("Found non-string value for key {}", key);
                    continue;
                }

                if ("player".equals(key)) {
                    result.icon = url;
                }
                result.other.put(key, url);
            }

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
