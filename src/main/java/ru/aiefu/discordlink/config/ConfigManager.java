package ru.aiefu.discordlink.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.Nullable;
import ru.aiefu.discordlink.discord.DiscordConfig;
import ru.aiefu.discordlink.discord.DiscordLink;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigManager {

    public void genDiscordLinkSettings(){
        if(!Files.exists(Paths.get("./config/discord-chat/discord-link.json"))) {
            String gson = new GsonBuilder().setPrettyPrinting().create().toJson(new DiscordConfig());
            File file = new File("./config/discord-chat/discord-link.json");
            fileWriter(file, gson);
        }
    }

    public void writeCurrentConfigInstance(){
        String gson = new GsonBuilder().setPrettyPrinting().create().toJson(DiscordLink.config);
        File file = new File("./config/discord-chat/discord-link.json");
        fileWriter(file, gson);
    }

    public DiscordConfig readDiscordLinkSettings() throws FileNotFoundException {
        return new Gson().fromJson(new FileReader("./config/discord-chat/discord-link.json"), DiscordConfig.class);
    }


    public void craftPaths() throws IOException {
        if(!Files.isDirectory(Paths.get("./config/discord-chat"))){
            Files.createDirectories(Paths.get("./config/discord-chat/linked-profiles"));
        }
    }

    @Nullable
    public static LinkedProfile getLinkedProfile(String uuid) throws FileNotFoundException {
        String path = String.format("./config/discord-chat/linked-profiles/%s.json", uuid);
        if(Files.exists(Paths.get(path))) {
            return new Gson().fromJson(new FileReader(path), LinkedProfile.class);
        } else return null;
    }

    public static void saveLinkedProfile(LinkedProfile profile){
        String gson = new GsonBuilder().setPrettyPrinting().create().toJson(profile);
        File file = new File(String.format("./config/discord-chat/linked-profiles/%s.json", profile.uuid));
        fileWriter(file, gson);
    }

    public static void fileWriter(File file, String gson){
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try(FileWriter writer = new FileWriter(file)) {
            writer.write(gson);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
