package com.kwvanderlinde.discordant.core;

import com.kwvanderlinde.discordant.core.config.ConfigManager;
import com.kwvanderlinde.discordant.core.linkedprofiles.ConfigProfileRepository;
import com.kwvanderlinde.discordant.core.discord.api.DiscordApi;
import com.kwvanderlinde.discordant.core.discord.api.JdaDiscordApi;
import com.kwvanderlinde.discordant.core.linkedprofiles.HashTableLinkedProfileRepository;
import com.kwvanderlinde.discordant.core.linkedprofiles.LinkedProfileManager;
import com.kwvanderlinde.discordant.core.discord.api.NullDiscordApi;
import com.kwvanderlinde.discordant.core.linkedprofiles.LinkedProfileRepository;
import com.kwvanderlinde.discordant.core.linkedprofiles.WriteThroughLinkedProfileRepository;
import com.kwvanderlinde.discordant.core.logging.DiscordantAppender;
import com.kwvanderlinde.discordant.core.config.DiscordantConfig;
import com.kwvanderlinde.discordant.core.messages.scopes.AdvancementScope;
import com.kwvanderlinde.discordant.core.messages.scopes.ChatScope;
import com.kwvanderlinde.discordant.core.messages.scopes.DeathScope;
import com.kwvanderlinde.discordant.core.messages.scopes.NilScope;
import com.kwvanderlinde.discordant.core.messages.scopes.NotificationStateScope;
import com.kwvanderlinde.discordant.core.messages.scopes.PendingVerificationScope;
import com.kwvanderlinde.discordant.core.modinterfaces.Integration;
import com.kwvanderlinde.discordant.core.modinterfaces.Server;
import com.kwvanderlinde.discordant.core.utils.TickedClock;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The main handle to the core API.
 *
 * Can be consumed by minecraft integrators in order to support new minecraft versions.
 */
// TODO Factor out the miscellaneous behaviour. E.g., we have config as a separate object, we should
//   also have link verification be its own thing, and scope building and embed building should be
//   something else as well.
public class Discordant {
    private static final Logger logger = LogManager.getLogger(Discordant.class);

    public static Discordant initialize(Integration integration) {
        // Initialize the services (config, cache, discord)

        try {
            return new Discordant(integration);
        }
        catch (ModLoadFailed modLoadFailed) {
            final var cause = modLoadFailed.getCause();
            if (cause instanceof ConfigurationValidationFailed configurationValidationFailed) {
                logger.error("Invalid discordant configuration: {}", configurationValidationFailed.getMessage());
            }
            else {
                logger.error("Failed to load Discordant mod: {}", modLoadFailed.getMessage(), modLoadFailed);
            }
            return null;
        }
    }

    private final TickedClock clock;
    private final ConfigManager configManager;
    private final LinkedProfileRepository linkedProfileRepository;
    private final EmbedFactory embedFactory;
    private final ServerCache serverCache;

    private @Nullable Server server;

    // region Configuration-dependent services.
    private DiscordApi discordApi = new NullDiscordApi();
    private DiscordantConfig config = new DiscordantConfig();
    private LinkedProfileManager linkedProfileManager;
    private DiscordantAppender logAppender;
    private ScopeFactory scopeFactory;
    // endregion

    private final Set<UUID> knownPlayerIds = new HashSet<>();
    private final Pattern mentionPattern = Pattern.compile("(?<=@).+?(?=@|$|\\s)");

    public Discordant(Integration minecraftIntegration) throws ModLoadFailed {
        this.clock = new TickedClock();

        final var configRoot = minecraftIntegration.getConfigRoot().resolve("discordant");
        linkedProfileRepository = new WriteThroughLinkedProfileRepository(
                new HashTableLinkedProfileRepository(),
                new ConfigProfileRepository(configRoot.resolve("linked-profiles"))
        );
        embedFactory = new EmbedFactory();

        this.serverCache = new FileBackedServerCache(configRoot.resolve("cache"));

        configManager = new ConfigManager(configRoot);
        try {
            configManager.ensureConfigStructure();
        }
        catch (IOException e) {
            throw new ModLoadFailed(e);
        }

        try {
            loadConfig();
        }
        catch (ConfigurationValidationFailed e) {
            throw new ModLoadFailed(e);
        }

        minecraftIntegration.enableCommands(config.linking.enabled && !config.linking.required);

        minecraftIntegration.events().onServerStarted((server) -> {
            Discordant.this.server = server;

            discordApi.addHandler(new DiscordantMessageHandler(
                    this,
                    linkedProfileManager,
                    scopeFactory,
                    embedFactory,
                    config,
                    server,
                    discordApi
            ));

            // TODO Attach server icon as a thumbnail or image if possible.
            final var message = config.discord.messages.serverStart
                    .instantiate(scopeFactory.serverScope(server));
            discordApi.sendEmbed(embedFactory.embedBuilder(message).build());

            // Update the channel topic.
            final var topic = config.discord.topics.channelTopic
                    .instantiate(scopeFactory.serverScope(server));
            if (topic.description != null) {
                discordApi.setTopic(topic.description);
            }
        });
        minecraftIntegration.events().onServerStopping((server) -> {
            {
                // Update the channel topic.
                final var topic = config.discord.topics.shutdownTopic
                        .instantiate(scopeFactory.serverScope(server));
                if (topic.description != null) {
                    discordApi.setTopic(topic.description);
                }
            }
            {
                final var message = config.discord.messages.serverStop
                        .instantiate(scopeFactory.serverScope(server));
                discordApi.sendEmbed(embedFactory.embedBuilder(message).build());
            }
        });
        minecraftIntegration.events().onServerStopped((server) -> {
            ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).removeAppender(logAppender);
            discordApi.close();
            discordApi = new NullDiscordApi();
        });
        minecraftIntegration.events().onTickStart((server) -> {
            clock.tick();
        });
        minecraftIntegration.events().onTickEnd(server -> {
            int tickCount = server.getTickCount();
            // TODO Configurable times for setting topic and expiring pending players. Actually, we
            //  don't even need to expire pending players, except to avoid stale data.
            if (tickCount % 6000 == 0) {
                // Update the channel topic.
                final var topic = config.discord.topics.channelTopic
                        .instantiate(scopeFactory.serverScope(server));
                if (topic.description != null) {
                    discordApi.setTopic(topic.description);
                }
            }
            if (tickCount % 1200 == 0) {
                linkedProfileManager.clearExpiredVerifications();
            }
        });

        minecraftIntegration.events().onPlayerSentMessage((player, chatMessage, plainTextCompositeMessage) -> {
            if (config.enableMentions) {
                chatMessage = parseDiscordMentions(chatMessage);
            }

            final var message = config.discord.messages.playerChat
                    .instantiate(new ChatScope(
                            // TODO Look up the linked profile and pass the corresponding discord user.
                            scopeFactory.playerScope(player.profile(), getServer(), null),
                            chatMessage
                    ));
            final var e = embedFactory.embedBuilder(message);

            e.setAuthor(player.name(), null, scopeFactory.getPlayerIconUrl(player.profile(), getServer()));
            {
                // If linking is enabled, actually use the discord details as the author.
                final var guild = discordApi.getGuild();
                if (config.linking.enabled && guild != null) {
                    final var discordId = linkedProfileManager.getDiscordIdForPlayerId(player.uuid());
                    if (discordId != null) {
                        Member m = guild.getMemberById(discordId);
                        if (m != null) {
                            e.setAuthor(m.getEffectiveName(), null, m.getEffectiveAvatarUrl());
                        }
                    }
                }
            }

            discordApi.sendEmbed(e.build());
        });

        minecraftIntegration.events().onPlayerJoinAttempt((server, profile, reject) -> {
            if (!config.linking.enabled) {
                // This handler is only for loading and checking linked accounts.
                return;
            }

            final var isLinked = linkedProfileManager.ensureProfileIsLinked(profile);
            if (!isLinked && config.linking.required) {
                // Profile does not exist. So send the user a code to verify with.
                final var authCode = linkedProfileManager.generateLinkCode(profile.uuid(), profile.name());
                final var message = config.minecraft.messages.verificationDisconnect
                        .instantiate(new PendingVerificationScope(scopeFactory.serverScope(server), authCode));
                reject.withReason(message);
            }
        });
        minecraftIntegration.events().onPlayerJoin((player) -> {
            knownPlayerIds.add(player.uuid());

            // TODO Look up the linked profile and pass the corresponding discord user.
            final var message = config.discord.messages.playerJoin
                    .instantiate(scopeFactory.playerScope(player.profile(), getServer(), null));

            discordApi.sendEmbed(embedFactory.embedBuilder(message).build());
        });
        minecraftIntegration.events().onPlayerDisconnect(player -> {
            if (!knownPlayerIds.contains(player.uuid())) {
                return;
            }

            // TODO Look up the linked profile and pass the corresponding discord user.
            final var message = config.discord.messages.playerLeave
                    .instantiate(scopeFactory.playerScope(player.profile(), getServer(), null));
            discordApi.sendEmbed(embedFactory.embedBuilder(message).build());

            knownPlayerIds.remove(player.uuid());
            // Remove in-memory profile linking. Will be reloaded next login if enabled.
            linkedProfileManager.unloadProfile(player.profile());
        });
        minecraftIntegration.events().onPlayerDeath((player, deathMessage) -> {
            final var message = config.discord.messages.playerDeath
                    .instantiate(new DeathScope(
                            scopeFactory.playerScope(player.profile(), getServer(), null),
                            deathMessage
                    ));
            discordApi.sendEmbed(embedFactory.embedBuilder(message).build());
        });
        minecraftIntegration.events().onPlayerAdvancement((player, advancement) -> {
            // TODO Look up the linked profile and pass the corresponding discord user.
            final var message = config.discord.messages.playerAdvancement
                    .instantiate(new AdvancementScope(
                            scopeFactory.playerScope(player.profile(), getServer(), null),
                            advancement.name(),
                            advancement.title(),
                            advancement.description()
                    ));
            discordApi.sendEmbed(embedFactory.embedBuilder(message).build());
        });

        final var commandHandlers = minecraftIntegration.commandsHandlers();
        commandHandlers.link = (player, respondWith) -> {
            // TODO If already linked, tell the user instead of generating a new code.
            final var authCode = linkedProfileManager.generateLinkCode(player.uuid(), player.name());
            final var scope = new PendingVerificationScope(scopeFactory.serverScope(getServer()), authCode);
            respondWith.success(config.minecraft.messages.commandLinkMsg.instantiate(scope));
        };
        commandHandlers.unlink = (player, respondWith) -> {
            final var wasDeleted = linkedProfileManager.removeLinkedProfile(player.uuid());
            if (wasDeleted) {
                final var component = config.minecraft.messages.codeUnlinkMsg
                        .instantiate(new NilScope());
                respondWith.success(component);
            }
            else {
                final var component = config.minecraft.messages.codeUnlinkFail
                        .instantiate(new NilScope());
                respondWith.failure(component);
            }
        };
        commandHandlers.queryMentionNotificationsEnabled = (player, respondWith) -> {
            final var state = player.isMentionNotificationsEnabled();
            final var message = config.minecraft.messages.mentionStateQueryResponse
                    .instantiate(new NotificationStateScope(state));
            respondWith.success(message);
        };
        commandHandlers.setMentionNotificationsEnabled = (player, enabled, respondWith) -> {
            player.setMentionNotificationsEnabled(enabled);
            final var message = config.minecraft.messages.mentionStateUpdateResponse
                    .instantiate(new NotificationStateScope(enabled));
            respondWith.success(message);
        };
    }

    private void loadConfig() throws ConfigurationValidationFailed {
        try {
            config = configManager.readDiscordLinkSettings();

            final var discordConfig = config.discord;
            if ("".equals(discordConfig.token) || null == discordConfig.token) {
                throw new ConfigurationValidationFailed("A bot token must be provided in config.json!");
            }
            if ("".equals(discordConfig.serverId) || null == discordConfig.serverId) {
                throw new ConfigurationValidationFailed("A server ID must be provided in config.json!");
            }
            if ("".equals(discordConfig.chatChannelId) || null == discordConfig.chatChannelId) {
                throw new ConfigurationValidationFailed("A chat channel ID must be provided in config.json!");
            }
            if (config.enableLogsForwarding && ("".equals(discordConfig.consoleChannelId) || null == discordConfig.consoleChannelId)) {
                throw new ConfigurationValidationFailed("A console channel ID must be provided in config.json when log forwarding is enabled!");
            }

            discordApi = new JdaDiscordApi(config, serverCache);
        }
        catch (ConfigurationValidationFailed e) {
            throw e;
        }
        catch (Exception e) {
            throw new ConfigurationValidationFailed("Unhandled exception", e);
        }

        linkedProfileManager = new LinkedProfileManager(clock, config.linking, linkedProfileRepository);
        scopeFactory = new ScopeFactory(clock, config, discordApi.getBotName());

        logAppender = new DiscordantAppender(Level.INFO, discordApi);
        ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).addAppender(logAppender);

        // TODO Need to disable commands based on new config.
        //minecraftIntegration.enableCommands(config.linking.enabled && !config.linking.required);
    }

    public @Nullable Server getServer() {
        return server;
    }

    private String parseDiscordMentions(String msg) {
        final var guild = discordApi.getGuild();
        if (guild != null) {
            List<String> mentions = mentionPattern.matcher(msg).results().map(matchResult -> matchResult.group(0)).toList();
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
}
