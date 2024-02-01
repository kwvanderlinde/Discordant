package com.kwvanderlinde.discordant.core.discord.api;

import com.kwvanderlinde.discordant.core.config.DiscordChannelConfig;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface MessageHandler {
    /**
     * Called whenever a discord message is sent on {@code e.getChannel()}.
     *
     * @param e
     * @param message
     */
    void onChannelInput(MessageReceivedEvent e, String message);

    void onBotPrivateMessage(MessageReceivedEvent e, String message);
}
