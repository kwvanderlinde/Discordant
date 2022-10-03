package com.kwvanderlinde.discordant.mc.discord;

import com.kwvanderlinde.discordant.core.config.DiscordConfig;
import com.kwvanderlinde.discordant.core.discord.api.DiscordApi;
import com.kwvanderlinde.discordant.mc.discord.msgparsers.DefaultParser;
import com.kwvanderlinde.discordant.mc.discord.msgparsers.MentionParser;
import com.kwvanderlinde.discordant.mc.discord.msgparsers.MsgParser;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

// TODO Delegate as much as possible to core logic rather than embedding it here. E.g., definitely
//  filling parameters from message configs is core logic, which includes looking up potential
//  linked profiles. Also any config-based decision making does not belong here as we want this to
//  be the slimmest shim you've ever seen.
// TODO More precisily, this class (along with DefaultParser and MentionParser) need to be
//  refactored in this way:
//  1. Identify the high-level goal of each.
//  2. Build the minecraft-specific logic on top of abstractions.
//  To put it more concretely, `handle(.*)Input()` is not the operations to build on. Rather, we
//  should parse, then fill in the logic as needed with plugable implementations. As a specific
//  example, in order to process command input, the listener essentially does this:
//  1. Check if the message is sent on the chat channel. If so, continue to (2) by delegating to `handleChatInput()`.
//  2. Check if the messages starts with `!` (meaning command), but not `!@` (meaning mention... possibly depending on the installed MsgParser). If so, continues to (3) by delegating to `handleCommandInput()` and stripping the leading `!`.
//  3. Parse the command (currently only "list") and send the results via the discord API to the channel that the message was sent on.
//  My proposal is to change the logic to look more like this for that case:
//  1. Parse the message. Parsing is dependent on which channel is used, but in the end produces a specific type of message structure.
//  2. Process the message. Have a different handler for each kind of message (chat command, console command, chat communication)
//  AND THEN We don't have to stuff all this logic into one class, but the message parser can instead return a parsed message that can then be handled elsewhere. The `DiscordListener` is responsible for bringing these bits together so that it can be called automatically by JDA.
public class DiscordListener extends ListenerAdapter {
    private final DiscordApi discordApi;
    private final DiscordConfig config;
    private final MsgParser chatHandler;

    public DiscordListener(DiscordApi discordApi, DiscordConfig config) {
        this.discordApi = discordApi;
        this.config = config;
        if (config.enableMentions) {
            chatHandler = new MentionParser();
        }
        else {
            chatHandler = new DefaultParser();
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        DedicatedServer server = DiscordantModInitializer.server;
        if (e.getAuthor() != e.getJDA().getSelfUser() && !e.getAuthor().isBot() && server != null) {
            String channelId = e.getChannel().getId();
            if (channelId.equals(config.chatChannelId)) {
                handleChatInput(e, server);
            }
            else if (channelId.equals(config.consoleChannelId)) {
                handleConsoleInput(e, server);
            }
            else if (config.enableAccountLinking && e.getChannelType() == ChannelType.PRIVATE) {
                try {
                    tryVerify(e, server);
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void handleChatInput(MessageReceivedEvent e, DedicatedServer server) {
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

    private void handleConsoleInput(MessageReceivedEvent e, DedicatedServer server) {
        String msg = e.getMessage().getContentRaw();
        DiscordantModInitializer.logger.info("Discord user " + e.getAuthor().getName() + " running command " + msg);
        server.execute(() -> server.handleConsoleInput(msg, server.createCommandSourceStack()));
    }

    private void handleCommandInput(MessageReceivedEvent e, DedicatedServer server, String command) {
        if (command.startsWith("list")) {
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            if (players.isEmpty()) {
                discordApi.sendMessage(e.getChannel(), config.noPlayersMsg);
                return;
            }
            StringBuilder sb = new StringBuilder(config.onlinePlayersMsg);
            for (ServerPlayer p : players) {
                sb.append(p.getScoreboardName()).append(", ");
            }
            discordApi.sendMessage(e.getChannel(), sb.substring(0, sb.length() - 2));
        }
    }

    private void tryVerify(MessageReceivedEvent e, DedicatedServer server) throws IOException {
        final var verified = DiscordantModInitializer.core.verifyLinkedProfile(e.getChannel(), e.getAuthor(), e.getMessage().getContentRaw());
    }

}
