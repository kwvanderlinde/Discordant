package com.kwvanderlinde.discordant.core.config;

import com.kwvanderlinde.discordant.core.messages.scopes.AdvancementScope;
import com.kwvanderlinde.discordant.core.messages.scopes.ChatScope;
import com.kwvanderlinde.discordant.core.messages.scopes.DeathScope;
import com.kwvanderlinde.discordant.core.messages.scopes.PlayerScope;
import com.kwvanderlinde.discordant.core.messages.scopes.ServerScope;

import java.awt.Color;

public class DiscordMessagesConfig {
    public DiscordMessageConfig<ServerScope> serverStart = new DiscordMessageConfig<>("{server.name} started", new Color(0x00ff00), null, null, "_{server.motd}_");
    public DiscordMessageConfig<ServerScope> serverStop = new DiscordMessageConfig<>("{server.name} stopped", new Color(0xff0000), null, null, null);
    public DiscordMessageConfig<PlayerScope> playerJoin = new DiscordMessageConfig<>("Player joined", new Color(0xFF2F97), null, "{player.avatarUrls|body}", "{player.name} joined the server!");
    public DiscordMessageConfig<PlayerScope> playerLeave = new DiscordMessageConfig<>("Player left", new Color(0x9F1D5E), null, null, "{player.name} left the server!");
    public DiscordMessageConfig<AdvancementScope> playerAdvancement = new DiscordMessageConfig<>("{player.name} has completed the challenge {advancement.title}", new Color(0xBF1AED), null, "{player.avatarUrls|head}", "{advancement.description}");
    public DiscordMessageConfig<DeathScope> playerDeath = new DiscordMessageConfig<>("The unthinkable has happened", new Color(0x000000), null, "{player.avatarUrls|head}", "{death.message}");
    public DiscordMessageConfig<ChatScope> playerChat = new DiscordMessageConfig<>(null, new Color(0x00ffff), null, null, "{chat.message}");
    public DiscordMessageConfig<PlayerScope> successfulVerification = new DiscordMessageConfig<>("Account linking successful!", new Color(0x92D22E), null, null, "Successfully linked discord account to your game account {player.name}({player.uuid})");
    public DiscordMessageConfig<PlayerScope> alreadyLinked = new DiscordMessageConfig<>("Account already linked", new Color(0x992D22), null, null, "Game account {player.name} is already linked to {player.discordName}");
    public DiscordMessageConfig<ServerScope> onlinePlayers = new DiscordMessageConfig<>(null, new Color(0x202020), null, null, "Currently there are {server.playerCount} / {server.maxPlayerCount} players on {server.name}.\n\n_Players online:_ {server.players}");
    public DiscordMessageConfig<ServerScope> noPlayers = new DiscordMessageConfig<>(null, new Color(0x202020), null, null, "Currently there are no players on {server.name}");
}
