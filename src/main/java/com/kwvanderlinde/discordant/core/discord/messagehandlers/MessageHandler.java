package com.kwvanderlinde.discordant.core.discord.messagehandlers;

import com.kwvanderlinde.discordant.core.modinterfaces.Server;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface MessageHandler {
    void handleChat(MessageReceivedEvent e, Server server, String msg);
}
