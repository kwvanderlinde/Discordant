package com.kwvanderlinde.discordant.mc.discord;

import com.kwvanderlinde.discordant.core.discord.*;
import com.kwvanderlinde.discordant.mc.DiscordantCommands;
import com.kwvanderlinde.discordant.mc.OnPlayerMessageEvent;
import com.kwvanderlinde.discordant.mc.ProfileLinkCommand;
import com.kwvanderlinde.discordant.core.config.ConfigManager;
import com.kwvanderlinde.discordant.core.logging.DiscordantAppender;
import com.kwvanderlinde.discordant.mc.language.ServerLanguage;
import kong.unirest.Unirest;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancements.Advancement;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.login.LoginException;
import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Discordant implements DedicatedServerModInitializer {
    public static DiscordApi discordApi = new NullDiscordApi();
    public static DiscordConfig config;
    public static LinkedProfileRepository linkedProfileRepository = new NullLinkedProfileRepository();
    public static DiscordantAppender logAppender;
    public static DedicatedServer server;
    public static Logger logger = LogManager.getLogger("Discordant");

    // TODO Fold link players into the LinkedProfile repository somehow.
    public static HashMap<String, LinkedProfile> linkedPlayers = new HashMap<>();
    public static HashMap<String, String> linkedPlayersByDiscordId = new HashMap<>();
    public static HashMap<Integer, VerificationData> pendingPlayers = new HashMap<>();
    public static HashMap<String, Integer> pendingPlayersUUID = new HashMap<>();
    public static long currentTime = System.currentTimeMillis();
    public static String botName;

    public static final ServerLanguage language = new ServerLanguage();

    @Override
    public void onInitializeServer() {
        ConfigManager manager = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve("discordant"));
        try {
            manager.ensureConfigStructure();
            Discordant.config = manager.readDiscordLinkSettings();
            Discordant.config.setup();
        }
        // TODO Hard failure on some of these exceptions. And by "hard" I mean don't initialize
        //  most mod functionality.
        catch (IOException e) {
            e.printStackTrace();
        }

        linkedProfileRepository = new ConfigProfileRepository(manager.getConfigRoot().resolve("linked-profiles"));

        try {
            discordApi = new JdaDiscordApi(config, linkedProfileRepository);

            logAppender = new DiscordantAppender(Level.INFO, discordApi);
            ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).addAppender(logAppender);

            botName = discordApi.getBotName();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        language.loadAllLanguagesIncludingModded(config.targetLocalization, config.isBidirectional);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            DiscordantCommands.register(dispatcher);
            if (config.enableAccountLinking && !config.forceLinking) {
                ProfileLinkCommand.register(dispatcher);
            }
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Discordant.server = (DedicatedServer) server;
            discordApi.postChatMessage(Discordant.config.startupMsg);
            OnPlayerMessageEvent.EVENT.register(Discordant::onPlayerMessage);
        });
        ServerTickEvents.START_SERVER_TICK.register(server -> currentTime = System.currentTimeMillis());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int tickCount = server.getTickCount();
            if (tickCount % 6000 == 0) {
                discordApi.setTopic(Discordant.config.channelTopicMsg + server.getPlayerCount() + "/" + server.getMaxPlayers());
            }
            if (tickCount % 1200 == 0) {
                for (Map.Entry<Integer, VerificationData> e : pendingPlayers.entrySet()) {
                    VerificationData data = e.getValue();
                    if (currentTime > data.validUntil()) {
                        pendingPlayersUUID.remove(data.uuid());
                        pendingPlayers.remove(e.getKey());
                    }
                }
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> shutdown());
    }

    public static void onPlayerMessage(ServerPlayer player, String msg, MutableComponent textComponent) {
        if (config.enableMentions) {
            msg = parseDiscordMentions(msg);
        }
        if (config.enableWebhook) {
            String uuid = player.getStringUUID();
            String name = player.getScoreboardName();
            final var guild = discordApi.getGuild();
            if (config.enableAccountLinking && guild != null && config.useDiscordData) {
                LinkedProfile profile = linkedPlayers.get(uuid);
                if (profile != null) {
                    Member m = guild.getMemberById(profile.discordId());
                    if (m != null) {
                        discordApi.postWebHookMsg(msg, m.getEffectiveName(), m.getEffectiveAvatarUrl());
                        return;
                    }
                }
            }
            discordApi.postWebHookMsg(msg, name, getPlayerIconUrl(name, uuid));
        }
        else {
            discordApi.postChatMessage(textComponent.getString());
        }
    }
    private static final Pattern pattern = Pattern.compile("(?<=@).+?(?=@|$|\\s)");

    public static String parseDiscordMentions(String msg) {
        final var guild = discordApi.getGuild();
        if (guild != null) {
            List<String> mentions = pattern.matcher(msg).results().map(matchResult -> matchResult.group(0)).toList();
            for (String s : mentions) {
                if (User.USER_TAG.matcher(s).matches()) {
                    Member m = guild.getMemberByTag(s);
                    if (m != null) {
                        msg = msg.replaceAll("@" + s, "<@!" + m.getId() + ">");
                    }
                }
            }
        }
        return msg;
    }

    public static void greetingMsg(String username, String uuid) {
        EmbedBuilder e = new EmbedBuilder();
        e.setAuthor(config.joinMessage.replaceAll("\\{username}", username), null, getPlayerIconUrl(username, uuid));
        e.setColor(Color.GREEN);
        discordApi.sendEmbed(e.build());
    }

    public static void logoutMsg(String username, String uuid) {
        EmbedBuilder e = new EmbedBuilder();
        e.setAuthor(config.disconnectMessage.replaceAll("\\{username}", username), null, getPlayerIconUrl(username, uuid));
        e.setColor(Color.RED);
        discordApi.sendEmbed(e.build());
    }

    public static void sendAdvancement(String username, Advancement adv, String uuid) {
        EmbedBuilder e = new EmbedBuilder();
        e.setAuthor(String.format(Language.getInstance().getOrDefault("chat.type.advancement." + adv.getDisplay().getFrame().getName()), username, adv.getDisplay().getTitle().getString()), null, getPlayerIconUrl(username, uuid));
        if (config.appendAdvancementDescription) {
            e.setDescription(String.format("** %s **", adv.getDisplay().getDescription().getString()));
        }
        e.setColor(12524269);
        discordApi.sendEmbed(e.build());
    }

    public static void sendDeathMsg(String username, String msg, String uuid) {
        EmbedBuilder e = new EmbedBuilder();
        e.setAuthor(msg, null, getPlayerIconUrl(username, uuid));
        e.setColor(Color.RED);
        discordApi.sendEmbed(e.build());
    }


    public static String getPlayerIconUrl(String name, String uuid) {
        return Discordant.config.playerHeadsUrl.replaceAll("\\{username}", name).replaceAll("\\{uuid}", uuid);
    }

    public void shutdown() {
        ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).removeAppender(logAppender);

        discordApi.setTopic(config.shutdownTopicMsg);
        discordApi.postChatMessage(config.serverStopMsg);

        // TODO Why sleep?
        try {
            Thread.sleep(350L);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        discordApi.close();
        discordApi = new NullDiscordApi();
        Unirest.shutDown();

    }
}
