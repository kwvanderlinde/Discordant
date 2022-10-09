package com.kwvanderlinde.discordant.core.config;

public class DiscordantConfig {
    public boolean enableLogsForwarding = true;

    public String playerHeadsUrl = "http://cravatar.eu/avatar/{uuid}/400.png";
    public boolean enableAccountLinking = true;
    public boolean forceLinking = false;
    public boolean enableMentions = true;

    public String targetLocalization = "en_us";
    public boolean isBidirectional = false;

    // TODO Make the new types initialize to reasonable defaults in the absence of deserialization.
    public DiscordConfig discord = new DiscordConfig();
    public MinecraftConfig minecraft = new MinecraftConfig();
}
