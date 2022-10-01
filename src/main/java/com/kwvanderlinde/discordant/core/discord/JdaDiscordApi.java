package com.kwvanderlinde.discordant.core.discord;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kwvanderlinde.discordant.mc.discord.DiscordConfig;
import com.kwvanderlinde.discordant.mc.discord.DiscordListener;
import kong.unirest.Unirest;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import okhttp3.OkHttpClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class JdaDiscordApi implements DiscordApi {
    private final @Nonnull JDA jda;
    private final @Nonnull DiscordConfig config;
    private final @Nonnull String botName;
    private final @Nullable TextChannel chatChannel;
    private final @Nullable TextChannel consoleChannel;
    private final @Nullable Guild guild;
    private final @Nullable Webhook webhook;
    // TODO Don't bother. Instead, replace this impl with a dummy impl.
    private boolean stopped = false;

    public JdaDiscordApi(@Nonnull DiscordConfig config) throws InterruptedException {
        this.config = config;

        jda = JDABuilder.createDefault(config.token)
                        .setHttpClient(new OkHttpClient.Builder().build())
                        .setMemberCachePolicy(MemberCachePolicy.ALL)
                        .enableIntents(GatewayIntent.GUILD_MEMBERS)
                        .addEventListeners(new Object[]{new DiscordListener(this)})
                        .build();
        jda.awaitReady();

        if (!config.serverId.isEmpty()) {
            guild = jda.getGuildById(config.serverId);
            if (guild != null && config.preloadDiscordMembers) {
                // TODO Do I need to keep the results? Does this call actually do anything? Does it cache internally or something?
                guild.loadMembers();
            }
        }
        else {
            guild = null;
        }
        botName = jda.getSelfUser().getName();
        chatChannel = jda.getTextChannelById(config.chatChannelId);
        consoleChannel = jda.getTextChannelById(config.consoleChannelId);

        if (config.enableWebhook && chatChannel != null) {
            webhook = chatChannel.createWebhook("Minecraft Chat Message Forwarding").complete();
        }
        else {
            webhook = null;
        }
    }

    public void sendMessage(@Nonnull MessageChannel ch, @Nonnull String msg) {
        if (!stopped) {
            ch.sendMessage(msg).queue();
        }
    }

    public void sendEmbed(@Nonnull MessageEmbed e) {
        if (!stopped && chatChannel != null) {
            chatChannel.sendMessageEmbeds(e).queue();
        }
    }

    @Override
    public void close() {
        stopped = true;
        jda.shutdown();
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
    public void postChatMessage(@Nonnull String msg) {
        if (!stopped && chatChannel != null) {
            sendMessage(chatChannel, msg);
        }
    }

    @Override
    public void postWebHookMsg(@Nonnull String msg, @Nonnull String username, @Nonnull String avatarUrl) {
        if (!stopped && webhook != null) {
            // TODO Does JDA not have a way to do what we need?
            JsonObject object = new JsonObject();
            object.addProperty("username", username);
            object.addProperty("avatar_url", avatarUrl);
            object.addProperty("content", msg);

            Unirest.post(webhook.getUrl())
                   .header("Content-type", "application/json")
                   .body(new Gson().toJson(object))
                   .asStringAsync();
        }
    }

    @Override
    public void setTopic(@Nonnull String msg) {
        if (!stopped && chatChannel != null) {
            chatChannel.getManager().setTopic(msg).queue();
        }
    }
}
