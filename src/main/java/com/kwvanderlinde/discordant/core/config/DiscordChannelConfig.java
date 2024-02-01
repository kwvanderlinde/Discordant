package com.kwvanderlinde.discordant.core.config;

import java.util.ArrayList;
import java.util.List;

public class DiscordChannelConfig {
    public String id = "";
    public List<String> tags = new ArrayList<>();
    public boolean enabled = true;
    /** {@code null} means no commands allowed; {@code ""} means everything is a command. */
    public String commandPrefix = null;
    public String mentionPrefix = null;
    public String topic = "Players online: {server.playerCount} / {server.maxPlayerCount} | Thank you to Crafatar for providing avatars: https://crafatar.com";
    public String shutdownTopic = "Server offline";
}
