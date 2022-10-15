package com.kwvanderlinde.discordant.core;

import com.kwvanderlinde.discordant.core.discord.api.DiscordApi;
import com.kwvanderlinde.discordant.core.discord.api.MessageListener;
import com.kwvanderlinde.discordant.core.discord.messagehandlers.MessageHandler;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class DiscordantMessageListener implements MessageListener {
    private static final Logger logger = LogManager.getLogger(DiscordantMessageListener.class);

    private final Discordant discordant;
    private final DiscordApi discordApi;
    private final MessageHandler chatHandler;

    public DiscordantMessageListener(Discordant discordant, DiscordApi discordApi, MessageHandler chatHandler) {
        this.discordant = discordant;
        this.discordApi = discordApi;
        this.chatHandler = chatHandler;
    }

    @Override
    public void onChatInput(MessageReceivedEvent e, String message) {
        String msg = e.getMessage().getContentRaw();
        if (!msg.isEmpty()) {
            if (!msg.startsWith("!@") && msg.startsWith("!")) {
                handleCommandInput(e, msg.substring(1));
            }
            else {
                chatHandler.handleChat(e, discordant.getServer(), msg);
            }
        }
    }

    @Override
    public void onConsoleInput(MessageReceivedEvent e, String message) {
        logger.info("Discord user " + e.getAuthor().getName() + " running command " + message);
        discordant.getServer().runCommand(message);
    }

    @Override
    public void onBotPmInput(MessageReceivedEvent e, String message) {
        final var config = discordant.getConfig();
        if (!config.linking.enabled) {
            return;
        }

        try {
            tryVerify(e);
        }
        catch (IOException ex) {
            // TODO Almost certainly not a desirable place to handle this failure.
            ex.printStackTrace();
        }
    }


    private void handleCommandInput(MessageReceivedEvent event, String command) {
        final var config = discordant.getConfig();
        final var server = discordant.getServer();

        if (command.startsWith("list")) {
            final var players = server.getAllPlayers().toList();
            if (players.isEmpty()) {
                final var message = config.discord.messages.noPlayers
                        .instantiate(discordant.serverScope(server));
                discordApi.sendEmbed(Discordant.buildMessageEmbed(message).build());
            }
            else {
                final var message = config.discord.messages.onlinePlayers
                        .instantiate(discordant.serverScope(server));

                discordApi.sendEmbed(Discordant.buildMessageEmbed(message).build());
            }
        }
    }

    private void tryVerify(MessageReceivedEvent e) throws IOException {
        final var verified = discordant.verifyLinkedProfile(e.getChannel(), e.getAuthor(), e.getMessage().getContentRaw());
    }
}
