package com.kwvanderlinde.discordant.core.config;

import com.kwvanderlinde.discordant.core.messages.scopes.AdvancementScope;
import com.kwvanderlinde.discordant.core.messages.scopes.ChatScope;
import com.kwvanderlinde.discordant.core.messages.scopes.DeathScope;
import com.kwvanderlinde.discordant.core.messages.scopes.PlayerScope;
import com.kwvanderlinde.discordant.core.messages.scopes.ServerScope;

import java.awt.Color;

public class DiscordMessagesConfig {
    public DiscordMessageConfig<ServerScope> serverStart = new DiscordMessageConfig<>("{server.name} started", new Color(0x00ff00), null, "> _{server.motd}_");
    public DiscordMessageConfig<ServerScope> serverStop = new DiscordMessageConfig<>("{server.name} stopped", new Color(0xff0000), null, null);
    public DiscordMessageConfig<PlayerScope> playerJoin = new DiscordMessageConfig<>("Player joined", new Color(0x00ff00), "{player.iconUrl}", "{username} joined the server!");
    public DiscordMessageConfig<PlayerScope> playerLeave = new DiscordMessageConfig<>("Player left", new Color(0xff0000), null, "{username} left the server!");
    public DiscordMessageConfig<AdvancementScope> playerAdvancement = new DiscordMessageConfig<>("{player.name} has completed the challenge {advancement.title}", new Color(0xBF1AED), "{player.iconUrl}", "{advancement.description}");
    public DiscordMessageConfig<DeathScope> playerDeath = new DiscordMessageConfig<>("The unthinkable has happened", new Color(0x000000), "{player.iconUrl}", "{death.message}");
    public DiscordMessageConfig<ChatScope> playerChat = new DiscordMessageConfig<>(null, new Color(0x00ffff), null, "{chat.message}");
    public DiscordMessageConfig<PlayerScope> successfulVerification = new DiscordMessageConfig<>("Account linking successful!", new Color(0x92D22E), null, "Successfully linked discord account to your game account {username}({uuid})");
    public DiscordMessageConfig<PlayerScope> alreadyLinked = new DiscordMessageConfig<>("Account already linked", new Color(0x992D22), null, "Game account {username} is already linked to {discordname}");
    public DiscordMessageConfig<ServerScope> onlinePlayers = new DiscordMessageConfig<>(null, new Color(0x202020), null, "Currently there are {server.playerCount} / {server.maxPlayerCount} players on {server.name}.\n\n_Players online:_ {server.players}");
    public DiscordMessageConfig<ServerScope> noPlayers = new DiscordMessageConfig<>(null, new Color(0x202020), null, "Currently there are no players on {server.name}");
}
