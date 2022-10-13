package com.kwvanderlinde.discordant.core.discord;

import com.kwvanderlinde.discordant.core.Discordant;
import com.kwvanderlinde.discordant.core.config.DiscordantConfig;
import com.kwvanderlinde.discordant.core.discord.api.DiscordApi;
import com.kwvanderlinde.discordant.core.discord.messagehandlers.DefaultMessageHandler;
import com.kwvanderlinde.discordant.core.discord.messagehandlers.MentionMessageHandler;
import com.kwvanderlinde.discordant.core.discord.messagehandlers.MessageHandler;
import com.kwvanderlinde.discordant.core.modinterfaces.Server;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;

public class DiscordListener extends ListenerAdapter {
    private static final Logger logger = LogManager.getLogger(DiscordListener.class);

    private final DiscordApi discordApi;
    private final Discordant discordant;
    private final DiscordantConfig config;
    private final MessageHandler chatHandler;

    public DiscordListener(DiscordApi discordApi, Discordant discordant, DiscordantConfig config) {
        this.discordApi = discordApi;
        this.discordant = discordant;
        this.config = config;

        if (config.enableMentions) {
            chatHandler = new MentionMessageHandler(discordant);
        }
        else {
            chatHandler = new DefaultMessageHandler();
        }
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        final var server = discordant.getServer();
        if (event.getAuthor() != event.getJDA().getSelfUser() && !event.getAuthor().isBot() && server != null) {
            final var channelId = event.getChannel().getId();

            if (channelId.equals(config.discord.chatChannelId)) {
                handleChatInput(event, server);
            }
            else if (channelId.equals(config.discord.consoleChannelId)) {
                handleConsoleInput(event, server);
            }
            else if (config.enableAccountLinking && event.getChannelType() == ChannelType.PRIVATE) {
                try {
                    tryVerify(event);
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void handleChatInput(MessageReceivedEvent e, Server server) {
        String msg = e.getMessage().getContentRaw();
        if (!msg.isEmpty()) {
            if (!msg.startsWith("!@") && msg.startsWith("!")) {
                handleCommandInput(e, server, msg.substring(1));
            }
            else {
                chatHandler.handleChat(e, server, msg);
            }
        }
    }

    private void handleConsoleInput(MessageReceivedEvent e, Server server) {
        String msg = e.getMessage().getContentRaw();
        logger.info("Discord user " + e.getAuthor().getName() + " running command " + msg);
        server.runCommand(msg);
    }

    private void handleCommandInput(MessageReceivedEvent event, Server server, String command) {
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
