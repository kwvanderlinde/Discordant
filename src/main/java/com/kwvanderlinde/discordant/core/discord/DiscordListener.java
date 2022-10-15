package com.kwvanderlinde.discordant.core.discord;

import com.kwvanderlinde.discordant.core.config.DiscordConfig;
import com.kwvanderlinde.discordant.core.discord.api.MessageListener;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class DiscordListener extends ListenerAdapter {
    private final List<MessageListener> messageListeners = new ArrayList<>();

    private final DiscordConfig config;

    public DiscordListener(DiscordConfig config) {
        this.config = config;
    }

    public void addListener(MessageListener messageListener) {
        this.messageListeners.add(messageListener);
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (event.getAuthor() != event.getJDA().getSelfUser() && !event.getAuthor().isBot()) {
            final var channelId = event.getChannel().getId();

            if (channelId.equals(config.chatChannelId)) {
                messageListeners.forEach(listener -> listener.onChatInput(event, event.getMessage().getContentRaw()));
            }
            else if (channelId.equals(config.consoleChannelId)) {
                messageListeners.forEach(listener -> listener.onConsoleInput(event, event.getMessage().getContentRaw()));
            }
            else if (event.getChannelType() == ChannelType.PRIVATE) {
                // TODO Is there some wierd way we could receive a PM that isn't to the bot?
                messageListeners.forEach(listener -> listener.onBotPmInput(event, event.getMessage().getContentRaw()));
            }
        }
    }
}
