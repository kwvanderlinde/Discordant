package com.kwvanderlinde.discordant.discord;

import com.kwvanderlinde.discordant.config.ConfigManager;
import com.kwvanderlinde.discordant.config.LinkedProfile;
import com.kwvanderlinde.discordant.discord.msgparsers.DefaultParser;
import com.kwvanderlinde.discordant.discord.msgparsers.MentionParser;
import com.kwvanderlinde.discordant.discord.msgparsers.MsgParser;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public class DiscordListener extends ListenerAdapter {

    private final MsgParser chatHandler;

    public DiscordListener() {
        if (Discordant.config.enableMentions) {
            chatHandler = new MentionParser();
        }
        else {
            chatHandler = new DefaultParser();
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        DedicatedServer server = Discordant.server;
        if (e.getAuthor() != e.getJDA().getSelfUser() && !e.getAuthor().isBot() && server != null) {
            String channelId = e.getChannel().getId();
            if (channelId.equals(Discordant.config.chatChannelId)) {
                handleChatInput(e, server);
            }
            else if (channelId.equals(Discordant.config.consoleChannelId)) {
                handleConsoleInput(e, server);
            }
            else if (Discordant.config.enableAccountLinking && e.getChannelType() == ChannelType.PRIVATE) {
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
        Discordant.logger.info("Discord user " + e.getAuthor().getName() + " running command " + msg);
        server.execute(() -> server.handleConsoleInput(msg, server.createCommandSourceStack()));
    }

    private void handleCommandInput(MessageReceivedEvent e, DedicatedServer server, String command) {
        if (command.startsWith("list")) {
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            if (players.isEmpty()) {
                Discordant.sendMessage(e.getChannel(), Discordant.config.noPlayersMsg);
                return;
            }
            StringBuilder sb = new StringBuilder(Discordant.config.onlinePlayersMsg);
            for (ServerPlayer p : players) {
                sb.append(p.getScoreboardName()).append(", ");
            }
            Discordant.sendMessage(e.getChannel(), sb.substring(0, sb.length() - 2));
        }
    }

    private void tryVerify(MessageReceivedEvent e, DedicatedServer server) throws IOException {
        String msg = e.getMessage().getContentRaw();
        if (msg.length() == 6 && msg.matches("[0-9]+")) {
            int code = Integer.parseInt(msg);
            VerificationData data = Discordant.pendingPlayers.get(code);
            if (data != null) {
                String id = data.uuid();
                if (!Files.exists(Paths.get(String.format("./config/discordant/linked-profiles/%s.json", id)))) {
                    String discordId = e.getAuthor().getId();
                    LinkedProfile profile = new LinkedProfile(data.name(), id, discordId);
                    ConfigManager.saveLinkedProfile(profile);
                    Discordant.pendingPlayersUUID.remove(id);
                    Discordant.pendingPlayers.remove(code);
                    if (!Discordant.config.forceLinking) {
                        ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(id));
                        if (player != null) {
                            Discordant.linkedPlayers.put(id, profile);
                            Discordant.linkedPlayersByDiscordId.put(profile.discordId, player.getGameProfile().getName());
                        }
                    }
                    e.getChannel().sendMessage(Discordant.config.successfulVerificationMsg
                                                       .replaceAll("\\{username}", data.name()).replaceAll("\\{uuid}", id)).queue();
                    Discordant.logger.info(Discordant.config.successLinkDiscordMsg.replaceAll("\\{username}", data.name()).replaceAll("\\{discordname}", e.getAuthor().getName()));
                }
                else {
                    LinkedProfile profile = ConfigManager.getLinkedProfile(id);
                    if (profile != null) {
                        Member m = Discordant.guild.getMemberById(profile.discordId);
                        String discordName = m != null ? m.getEffectiveName() : "Unknown user";
                        e.getChannel().sendMessage(Discordant.config.alreadyLinked.replaceAll("\\{username}", profile.name).replaceAll("\\{discordname}", discordName)).queue();
                        Discordant.pendingPlayersUUID.remove(id);
                        Discordant.pendingPlayers.remove(code);
                    }
                }
            }
        }
    }

}
