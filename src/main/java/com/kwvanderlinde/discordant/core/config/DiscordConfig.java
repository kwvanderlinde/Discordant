package com.kwvanderlinde.discordant.core.config;

public class DiscordConfig {
    public String token = "";
    public String serverId = "";
    public String chatChannelId = "";
    public String consoleChannelId = "";
    public boolean enableLogsForwarding = true;
    public DiscordMessagesConfig messages = new DiscordMessagesConfig();
    public DiscordTopicsConfig topics = new DiscordTopicsConfig();
}
