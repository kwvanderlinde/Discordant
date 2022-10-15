package com.kwvanderlinde.discordant.core.discord.api;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface MessageListener {
    void onChatInput(MessageReceivedEvent e, String message);

    void onConsoleInput(MessageReceivedEvent e, String message);

    void onBotPmInput(MessageReceivedEvent e, String message);
}
