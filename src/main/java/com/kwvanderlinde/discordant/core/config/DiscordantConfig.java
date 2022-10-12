package com.kwvanderlinde.discordant.core.config;

import java.util.Map;

public class DiscordantConfig {
    public boolean enableLogsForwarding = true;

    public String playerIconUrl = "https://crafatar.com/avatars/{uuid}/?size=16&overlay&ts={time}";
    public boolean enableAccountLinking = true;
    public boolean forceLinking = false;
    public boolean enableMentions = true;

    public String targetLocalization = "en_us";
    public boolean isBidirectional = false;

    public Map<String, String> avatarUrls = Map.of(
            "head", "https://crafatar.com/renders/head/{uuid}/?scale=10&overlay&ts={time}",
            "body", "https://crafatar.com/renders/body/{uuid}/?scale=10&overlay&ts={time}"
    );

    public DiscordConfig discord = new DiscordConfig();
    public MinecraftConfig minecraft = new MinecraftConfig();
}
