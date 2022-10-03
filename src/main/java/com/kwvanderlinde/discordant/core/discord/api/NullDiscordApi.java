package com.kwvanderlinde.discordant.core.discord.api;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NullDiscordApi implements DiscordApi {
    @Override
    public void close() {}

    @NotNull
    @Override
    public String getBotName() {
        return "null";
    }

    @Nullable
    @Override
    public Guild getGuild() {
        return null;
    }

    @Override
    public void sendMessage(@NotNull MessageChannel ch, @NotNull String msg) {

    }

    @Override
    public void sendEmbed(@NotNull MessageEmbed e) {

    }

    @Override
    public void postConsoleMessage(@NotNull String msg) {
    }

    @Override
    public void postChatMessage(@NotNull String msg) {
    }

    @Override
    public void postWebHookMsg(@NotNull String msg, @NotNull String username, @NotNull String avatarUrl) {
    }

    @Override
    public void setTopic(@NotNull String msg) {
    }
}
