package com.kwvanderlinde.discordant.core.config;

import java.util.LinkedHashMap;
import java.util.Map;

public class DiscordConfig {
    public String token = "";
    public String serverId = "";

    // TODO Moving to customized channels.
    //  When done, will not need a separate enableLogsForwarding configuration as I can just disable
    //  the log channel if any.

    public Map<String, DiscordChannelConfig> channels = new LinkedHashMap<>();

    public String chatChannelId = "";
    public String consoleChannelId = "";
    public boolean enableLogsForwarding = true;
    public boolean enableMentions = true;

    public DiscordMessagesConfig messages = new DiscordMessagesConfig();
    public DiscordTopicsConfig topics = new DiscordTopicsConfig();
}
