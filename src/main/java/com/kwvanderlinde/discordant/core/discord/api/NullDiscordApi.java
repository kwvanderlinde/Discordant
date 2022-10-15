package com.kwvanderlinde.discordant.core.discord.api;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class NullDiscordApi implements DiscordApi {
    @Override
    public void addListener(MessageHandler messageHandler) {
    }

    @Override
    public void close() {}

    @Override
    public @Nonnull String getBotName() {
        return "null";
    }

    @Override
    public @Nullable Guild getGuild() {
        return null;
    }

    @Override
    public void sendMessage(@Nonnull MessageChannel ch, @Nonnull String msg) {

    }

    @Override
    public void sendEmbed(@Nonnull MessageEmbed e) {

    }

    @Override
    public void sendEmbed(@Nonnull MessageChannel ch, @Nonnull MessageEmbed e) {

    }

    @Override
    public void postConsoleMessage(@Nonnull String msg) {
    }

    @Override
    public void setTopic(@Nonnull String msg) {
    }
}
