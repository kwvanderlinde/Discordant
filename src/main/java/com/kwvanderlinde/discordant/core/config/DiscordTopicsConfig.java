package com.kwvanderlinde.discordant.core.config;

import com.kwvanderlinde.discordant.core.messages.scopes.ServerScope;

public class DiscordTopicsConfig {
    public TopicConfig<ServerScope> channelTopic = new TopicConfig<>("Players online: {server.playerCount} / {server.maxPlayerCount}");
    public TopicConfig<ServerScope> shutdownTopic = new TopicConfig<>("Server offline");
}
