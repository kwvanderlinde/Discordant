package com.kwvanderlinde.discordant.core.config;

public class DiscordConfig {
    public String token = "";
    public String chatChannelId = "";
    public String consoleChannelId = "";
    public String serverId = "";

    public DiscordMessagesConfig messages = new DiscordMessagesConfig();
    public DiscordTopicsConfig topics = new DiscordTopicsConfig();
}
