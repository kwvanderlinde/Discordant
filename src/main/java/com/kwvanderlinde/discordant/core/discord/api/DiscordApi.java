package com.kwvanderlinde.discordant.core.discord.api;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface DiscordApi {
    void addHandler(MessageHandler messageHandler);

    void close();

    @Nonnull String getBotName();

    // TODO Expose linked profiles instead of a guild directly.
    @Nullable Guild getGuild();

    // TODO Allow any number of targets for both console and chat messages (and embeds), each with
    //  their own configuration, messages, support for webhooks, and topics.

    void sendEmbed(@Nonnull MessageEmbed e);

    // TODO I don't like this method being public. I want to just send messages and all applicable channels get it.
    //  This is really only used to respond to command in discord via listener. There is probably a better way than
    //  cluttering our core API.
    void sendEmbed(@Nonnull MessageChannel ch, @Nonnull MessageEmbed e);

    void postConsoleMessage(@Nonnull String msg);

    void setTopic(@Nonnull String msg);
}
