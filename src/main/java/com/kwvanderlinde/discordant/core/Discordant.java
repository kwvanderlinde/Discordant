package com.kwvanderlinde.discordant.core;

import com.kwvanderlinde.discordant.core.config.ConfigManager;
import com.kwvanderlinde.discordant.core.discord.api.ReplaceableDiscordApi;
import com.kwvanderlinde.discordant.core.linkedprofiles.ConfigProfileRepository;
import com.kwvanderlinde.discordant.core.discord.api.JdaDiscordApi;
import com.kwvanderlinde.discordant.core.linkedprofiles.HashTableLinkedProfileRepository;
import com.kwvanderlinde.discordant.core.linkedprofiles.LinkedProfileManager;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileNotFoundException;
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

    private final Integration minecraftIntegration;
    private final TickedClock clock;
    private final ConfigManager configManager;
    private final LinkedProfileRepository linkedProfileRepository;
    private final EmbedFactory embedFactory;

    private @Nullable Server server;

    private @Nonnull DiscordantConfig config;
    // region Reloadable components
    private @Nonnull ReplaceableDiscordApi discordApi;
    private @Nonnull LinkedProfileManager linkedProfileManager;
    private @Nonnull ScopeFactory scopeFactory;
    private @Nonnull DiscordantMessageHandler discordantMessageHandler;
    // endregion
    private final @Nonnull DiscordantAppender logAppender;

    private final Set<UUID> knownPlayerIds = new HashSet<>();
    private final Pattern mentionPattern = Pattern.compile("(?<=@).+?(?=@|$|\\s)");

    public Discordant(Integration minecraftIntegration) throws ModLoadFailed {
        this.minecraftIntegration = minecraftIntegration;
        this.clock = new TickedClock();

        final var configRoot = minecraftIntegration.getConfigRoot().resolve("discordant");
        linkedProfileRepository = new WriteThroughLinkedProfileRepository(
                new HashTableLinkedProfileRepository(),
                new ConfigProfileRepository(configRoot.resolve("linked-profiles"))
        );
        embedFactory = new EmbedFactory();

        configManager = new ConfigManager(configRoot);
        try {
            configManager.ensureConfigStructure();
            // TODO Initialize mod to a valid state where a reload can still be issued.
        }
        catch (IOException e) {
            throw new ModLoadFailed(e);
        }

        try {
            config = loadAndValidateConfig();
        }
        catch (ConfigurationValidationFailed e) {
            // TODO Initialize mod to a valid state where a reload can still be issued.
            throw new ModLoadFailed(e);
        }

        // Create new config-dependent components.
        discordApi = new ReplaceableDiscordApi(JdaDiscordApi::new, config);
        linkedProfileManager = new LinkedProfileManager(config.linking, clock, linkedProfileRepository);
        scopeFactory = new ScopeFactory(config, clock, discordApi.getBotName());
        discordantMessageHandler = new DiscordantMessageHandler(
                config,
                linkedProfileManager,
                scopeFactory,
                embedFactory,
                server,
                discordApi
        );

        logAppender = new DiscordantAppender(Level.INFO, discordApi);
        ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).addAppender(logAppender);

        minecraftIntegration.setLinkingCommandsEnabled(config.linking.enabled && !config.linking.required);

        minecraftIntegration.events().onServerStarted((server) -> {
            Discordant.this.server = server;

            discordApi.addHandler(discordantMessageHandler);

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
                            scopeFactory.playerScope(player.profile(), server, null),
                            chatMessage
                    ));
            final var e = embedFactory.embedBuilder(message);

            e.setAuthor(player.name(), null, scopeFactory.getPlayerIconUrl(player.profile(), server));
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
                    .instantiate(scopeFactory.playerScope(player.profile(), server, null));

            discordApi.sendEmbed(embedFactory.embedBuilder(message).build());
        });
        minecraftIntegration.events().onPlayerDisconnect(player -> {
            if (!knownPlayerIds.contains(player.uuid())) {
                return;
            }

            // TODO Look up the linked profile and pass the corresponding discord user.
            final var message = config.discord.messages.playerLeave
                    .instantiate(scopeFactory.playerScope(player.profile(), server, null));
            discordApi.sendEmbed(embedFactory.embedBuilder(message).build());

            knownPlayerIds.remove(player.uuid());
            // Remove in-memory profile linking. Will be reloaded next login if enabled.
            linkedProfileManager.unloadProfile(player.profile());
        });
        minecraftIntegration.events().onPlayerDeath((player, deathMessage) -> {
            final var message = config.discord.messages.playerDeath
                    .instantiate(new DeathScope(
                            scopeFactory.playerScope(player.profile(), server, null),
                            deathMessage
                    ));
            discordApi.sendEmbed(embedFactory.embedBuilder(message).build());
        });
        minecraftIntegration.events().onPlayerAdvancement((player, advancement) -> {
            // TODO Look up the linked profile and pass the corresponding discord user.
            final var message = config.discord.messages.playerAdvancement
                    .instantiate(new AdvancementScope(
                            scopeFactory.playerScope(player.profile(), server, null),
                            advancement.name(),
                            advancement.title(),
                            advancement.description()
                    ));
            discordApi.sendEmbed(embedFactory.embedBuilder(message).build());
        });

        final var commandHandlers = minecraftIntegration.commandsHandlers();
        commandHandlers.reload = () -> {
            final DiscordantConfig newConfig;
            try {
                newConfig = loadAndValidateConfig();
            }
            catch (ConfigurationValidationFailed e) {
                logger.warn("Reloaded configuration is invalid: {}", e.getMessage());
                return;
            }

            config = newConfig;
            linkedProfileManager.reload(newConfig);
            scopeFactory.reload(newConfig);
            discordApi.reload(newConfig);
            discordantMessageHandler.reload(newConfig);
        };
        commandHandlers.link = (player, respondWith) -> {
            // TODO If already linked, tell the user instead of generating a new code.
            final var authCode = linkedProfileManager.generateLinkCode(player.uuid(), player.name());
            final var scope = new PendingVerificationScope(scopeFactory.serverScope(server), authCode);
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

    private DiscordantConfig loadAndValidateConfig() throws ConfigurationValidationFailed {
        try {
            final var config = configManager.readConfigSettings();

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
            if (discordConfig.enableLogsForwarding && ("".equals(discordConfig.consoleChannelId) || null == discordConfig.consoleChannelId)) {
                throw new ConfigurationValidationFailed("A console channel ID must be provided in config.json when log forwarding is enabled!");
            }

            return config;
        }
        catch (ConfigurationValidationFailed e) {
            throw e;
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
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
