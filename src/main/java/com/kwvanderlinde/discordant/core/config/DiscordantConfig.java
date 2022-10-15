package com.kwvanderlinde.discordant.core.config;

import java.util.Map;

public class DiscordantConfig {
    public boolean enableLogsForwarding = true;

    public String playerIconUrl = "https://crafatar.com/avatars/{player.uuid}/?size=16&overlay&ts={server.time}";
    public boolean enableMentions = true;

    public Map<String, String> avatarUrls = Map.of(
            "head", "https://crafatar.com/renders/head/{player.uuid}/?scale=10&overlay&ts={server.time}",
            "body", "https://crafatar.com/renders/body/{player.uuid}/?scale=10&overlay&ts={server.time}"
    );

    public LinkingConfig linking = new LinkingConfig();
    public DiscordConfig discord = new DiscordConfig();
    public MinecraftConfig minecraft = new MinecraftConfig();
}
