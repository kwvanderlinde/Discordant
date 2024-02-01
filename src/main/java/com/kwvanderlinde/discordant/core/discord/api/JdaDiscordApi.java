package com.kwvanderlinde.discordant.core.discord.api;

import com.kwvanderlinde.discordant.core.config.DiscordChannelConfig;
import com.kwvanderlinde.discordant.core.config.DiscordConfig;
import com.kwvanderlinde.discordant.core.config.DiscordantConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
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
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

// TODO Prefer an active object design where we have an encapsulated JDA thread. That way server
//  init not dependent on JDA intiialization.
public class JdaDiscordApi implements DiscordApi {
    private static final Logger logger = LogManager.getLogger(JdaDiscordApi.class);

    private record ChannelHolder(@Nonnull String id, @Nonnull DiscordChannelConfig config, @Nonnull TextChannel channel) {}

    private final List<MessageHandler> messageHandlers = new ArrayList<>();
    private final @Nonnull JDA jda;
    private final @Nonnull DiscordConfig config;
    private final @Nonnull String botName;
    private final List<TextChannel> channels;
    private final @Nullable TextChannel chatChannel;
    private final @Nullable TextChannel consoleChannel;
    private final @Nullable Guild guild;
    private boolean handleRateLimitations = true;

    private final List<ChannelHolder> configuredChannels = new ArrayList<>();

    public JdaDiscordApi(@Nonnull DiscordantConfig config) {
        this.config = config.discord;

        jda = JDABuilder.createDefault(this.config.token)
                        .setHttpClient(new OkHttpClient.Builder().build())
                        .setMemberCachePolicy(MemberCachePolicy.ALL)
                        .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
                        .addEventListeners(new MessageReceivedListener())
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

            channels = new ArrayList<>();
            for (final var channelConfigEntry : this.config.channels.entrySet()) {
                final var channelId = channelConfigEntry.getKey();
                final var channelConfig = channelConfigEntry.getValue();

                if (!channelConfig.enabled) {
                    continue;
                }

                final var channel = jda.getTextChannelById(channelId);
                if (channel == null) {
                    continue;
                }

                channels.add(channel);
            }

            chatChannel = jda.getTextChannelById(this.config.chatChannelId);
            consoleChannel = this.config.enableLogsForwarding
                    ? jda.getTextChannelById(this.config.consoleChannelId)
                    : null;

            for (final var entry : this.config.channels.entrySet()) {
                final var channelId = entry.getKey();
                final var channelConfig = entry.getValue();
                final var channel = jda.getTextChannelById(botName);
                if (channel == null) {
                    logger.error("Unable to find channel with ID {}", channelId);
                    continue;
                }
                configuredChannels.add(new ChannelHolder(channelId, channelConfig, channel));
            }

        }
        catch (Exception e) {
            // Failed to load JDA somehow, so make sure it doesn't hang around and mess with the
            // process.
            jda.shutdownNow();
            throw e;
        }
    }

    // TODO Only support sending to explicitly ID'd channels. Selection logic is for another component.

    @Override
    public void sendEmbed(@Nonnull MessageEmbed e) {
        for (final var channelHolder : configuredChannels) {
            if (!channelHolder.config().tags.contains("embed")) {
                continue;
            }
            sendEmbed(channelHolder.channel(), e);
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
    public @Nullable User getUserById(String userId) {
        return jda.getUserById(userId);
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

    @Override
    public void setTopic(@Nonnull String channelId, @Nonnull String msg) {
        getChannel(channelId).ifPresent(configuredChannel -> configuredChannel.channel().getManager().setTopic(msg).submit(handleRateLimitations));
    }

    private Optional<ChannelHolder> getChannel(String channelId) {
        return configuredChannels.stream().filter(holder -> holder.id.equals(channelId)).findFirst();
    }

    private final class MessageReceivedListener extends ListenerAdapter {
        @Override
        public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
            if (event.getAuthor() == event.getJDA().getSelfUser() || event.getAuthor().isBot()) {
                // Don't respond to bots, including ourselves.
                return;
            }

            if (event.getChannelType() == ChannelType.PRIVATE) {
                // Not a configured channel, but private message with bot for linking.
                messageHandlers.forEach(handler -> handler.onBotPrivateMessage(event, event.getMessage().getContentRaw()));
            }
            else {
                messageHandlers.forEach(handler -> handler.onChannelInput(event, event.getMessage().getContentRaw()));
            }
        }
    }
}