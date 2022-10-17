package com.kwvanderlinde.discordant.core.discord.api;

import com.kwvanderlinde.discordant.core.config.DiscordConfig;
import com.kwvanderlinde.discordant.core.config.DiscordantConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class JdaDiscordApi implements DiscordApi {
    private static final Logger logger = LogManager.getLogger(JdaDiscordApi.class);

    private final List<MessageHandler> messageHandlers = new ArrayList<>();
    private final @Nonnull JDA jda;
    private final @Nonnull DiscordConfig config;
    private final @Nonnull String botName;
    private final @Nullable TextChannel chatChannel;
    private final @Nullable TextChannel consoleChannel;
    private final @Nullable Guild guild;
    private boolean handleRateLimitations = true;

    public JdaDiscordApi(@Nonnull DiscordantConfig config) {
        this.config = config.discord;

        final var listener = new ListenerAdapter() {
            @Override
            public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
                if (event.getAuthor() == event.getJDA().getSelfUser() || event.getAuthor().isBot()) {
                    // Don't respond to bots, including ourselves.
                    return;
                }

                final var channelId = event.getChannel().getId();
                if (channelId.equals(JdaDiscordApi.this.config.chatChannelId)) {
                    messageHandlers.forEach(handler -> handler.onChatInput(event, event.getMessage().getContentRaw()));
                }
                else if (channelId.equals(JdaDiscordApi.this.config.consoleChannelId)) {
                    messageHandlers.forEach(handler -> handler.onConsoleInput(event, event.getMessage().getContentRaw()));
                }
                else if (event.getChannelType() == ChannelType.PRIVATE) {
                    messageHandlers.forEach(handler -> handler.onBotPmInput(event, event.getMessage().getContentRaw()));
                }
            }
        };
        jda = JDABuilder.createDefault(this.config.token)
                        .setHttpClient(new OkHttpClient.Builder().build())
                        .setMemberCachePolicy(MemberCachePolicy.ALL)
                        .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
                        .addEventListeners(new Object[]{ listener })
                        .build();
        try {
            jda.awaitReady();
        }
        catch (InterruptedException e) {
            logger.warn(e);
        }

        try {
            if (!this.config.serverId.isEmpty()) {
                guild = jda.getGuildById(this.config.serverId);
                if (guild != null) {
                    guild.loadMembers();
                }
            }
            else {
                guild = null;
            }
            botName = jda.getSelfUser().getName();
            chatChannel = jda.getTextChannelById(this.config.chatChannelId);
            consoleChannel = this.config.enableLogsForwarding
                    ? jda.getTextChannelById(this.config.consoleChannelId)
                    : null;
        }
        catch (Exception e) {
            // Failed to load JDA somehow, so make sure it doesn't hang around and mess with the
            // process.
            jda.shutdownNow();
            throw e;
        }
    }

    @Override
    public void sendEmbed(@Nonnull MessageEmbed e) {
        if (chatChannel != null) {
            sendEmbed(chatChannel, e);
        }
    }

    @Override
    public void sendEmbed(@Nonnull MessageChannel ch, @Nonnull MessageEmbed e) {
        ch.sendMessageEmbeds(e).submit(handleRateLimitations);
    }

    @Override
    public void addHandler(MessageHandler messageHandler) {
        this.messageHandlers.add(messageHandler);
    }

    @Override
    public void close() {
        // Give JDA a short time to finish up. But don't let it sit there forever, it's not worth it.
        jda.shutdown();

        final var timer = new Timer(true);
        timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            logger.info("JDA took too long to shut down. Cancelling requests");
                            final var numberOfRequests = jda.cancelRequests();
                            logger.info("Cancelled {} requests", numberOfRequests);
                            jda.shutdownNow();
                        }
                        finally {
                            timer.cancel();
                        }
                    }
                },
                3000
        );
    }

    @Override
    @Nonnull
    public String getBotName() {
        return botName;
    }

    @Override
    @Nullable
    public Guild getGuild() {
        return guild;
    }

    @Override
    public void postConsoleMessage(@Nonnull String msg) {
        if (config.enableLogsForwarding && consoleChannel != null) {
            if (msg.length() > 1999) {
                msg = msg.substring(0, 1999);
            }
            consoleChannel.sendMessage(msg).submit(handleRateLimitations);
        }
    }

    @Override
    public void setTopic(@Nonnull String msg) {
        if (chatChannel != null) {
            chatChannel.getManager().setTopic(msg).submit(handleRateLimitations);
        }
    }
}
