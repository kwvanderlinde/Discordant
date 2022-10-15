package com.kwvanderlinde.discordant.core.discord.api;

import com.kwvanderlinde.discordant.core.Discordant;
import com.kwvanderlinde.discordant.core.ServerCache;
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
    private final @Nonnull DiscordantConfig config;
    private final @Nonnull String botName;
    private final @Nullable TextChannel chatChannel;
    private final @Nullable TextChannel consoleChannel;
    private final @Nullable Guild guild;
    private boolean handleRateLimitations = true;
    // TODO Don't bother. Instead, replace this impl with a dummy impl.
    private boolean stopped = false;

    public JdaDiscordApi(@Nonnull DiscordantConfig config, @Nonnull ServerCache cache) throws InterruptedException {
        this.config = config;

        final var listener = new ListenerAdapter() {
            @Override
            public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
                if (event.getAuthor() == event.getJDA().getSelfUser() || event.getAuthor().isBot()) {
                    // Don't respond to bots, including ourselves.
                    return;
                }

                final var channelId = event.getChannel().getId();
                if (channelId.equals(config.discord.chatChannelId)) {
                    messageHandlers.forEach(handler -> handler.onChatInput(event, event.getMessage().getContentRaw()));
                }
                else if (channelId.equals(config.discord.consoleChannelId)) {
                    messageHandlers.forEach(handler -> handler.onConsoleInput(event, event.getMessage().getContentRaw()));
                }
                else if (event.getChannelType() == ChannelType.PRIVATE) {
                    messageHandlers.forEach(handler -> handler.onBotPmInput(event, event.getMessage().getContentRaw()));
                }
            }
        };
        jda = JDABuilder.createDefault(config.discord.token)
                        .setHttpClient(new OkHttpClient.Builder().build())
                        .setMemberCachePolicy(MemberCachePolicy.ALL)
                        .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
                        .addEventListeners(new Object[]{ listener })
                        .build();
        jda.awaitReady();

        try {
            if (!config.discord.serverId.isEmpty()) {
                guild = jda.getGuildById(config.discord.serverId);
                if (guild != null) {
                    guild.loadMembers();
                }
            }
            else {
                guild = null;
            }
            botName = jda.getSelfUser().getName();
            chatChannel = jda.getTextChannelById(config.discord.chatChannelId);
            consoleChannel = config.enableLogsForwarding
                    ? jda.getTextChannelById(config.discord.consoleChannelId)
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
    public void sendMessage(@Nonnull MessageChannel ch, @Nonnull String msg) {
        if (!stopped) {
            ch.sendMessage(msg).submit(handleRateLimitations);
        }
    }

    @Override
    public void sendEmbed(@Nonnull MessageEmbed e) {
        if (!stopped && chatChannel != null) {
            chatChannel.sendMessageEmbeds(e).submit(handleRateLimitations);
        }
    }

    @Override
    public void sendEmbed(@Nonnull MessageChannel ch, @Nonnull MessageEmbed e) {
        if (!stopped) {
            ch.sendMessageEmbeds(e).submit(handleRateLimitations);
        }
    }

    @Override
    public void addHandler(MessageHandler messageHandler) {
        this.messageHandlers.add(messageHandler);
    }

    @Override
    public void close() {
        stopped = true;
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
        if (!stopped && config.enableLogsForwarding && consoleChannel != null) {
            if (msg.length() > 1999) {
                msg = msg.substring(0, 1999);
            }
            sendMessage(consoleChannel, msg);
        }
    }

    @Override
    public void setTopic(@Nonnull String msg) {
        if (!stopped && chatChannel != null) {
            chatChannel.getManager().setTopic(msg).submit(handleRateLimitations);
        }
    }
}
