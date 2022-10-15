package com.kwvanderlinde.discordant.core;

import com.kwvanderlinde.discordant.core.config.ConfigManager;
import com.kwvanderlinde.discordant.core.config.DiscordMessageConfig;
import com.kwvanderlinde.discordant.core.discord.linkedprofiles.ConfigProfileRepository;
import com.kwvanderlinde.discordant.core.discord.api.DiscordApi;
import com.kwvanderlinde.discordant.core.discord.api.JdaDiscordApi;
import com.kwvanderlinde.discordant.core.discord.linkedprofiles.HashTableLinkedProfileRepository;
import com.kwvanderlinde.discordant.core.discord.linkedprofiles.LinkedProfile;
import com.kwvanderlinde.discordant.core.discord.linkedprofiles.LinkedProfileRepository;
import com.kwvanderlinde.discordant.core.discord.api.NullDiscordApi;
import com.kwvanderlinde.discordant.core.discord.linkedprofiles.NullLinkedProfileRepository;
import com.kwvanderlinde.discordant.core.discord.linkedprofiles.WriteThroughLinkedProfileRepository;
import com.kwvanderlinde.discordant.core.logging.DiscordantAppender;
import com.kwvanderlinde.discordant.core.config.DiscordantConfig;
import com.kwvanderlinde.discordant.core.discord.linkedprofiles.VerificationData;
import com.kwvanderlinde.discordant.core.messages.PlainTextRenderer;
import com.kwvanderlinde.discordant.core.messages.ScopeUtil;
import com.kwvanderlinde.discordant.core.messages.scopes.AdvancementScope;
import com.kwvanderlinde.discordant.core.messages.scopes.ChatScope;
import com.kwvanderlinde.discordant.core.messages.scopes.DeathScope;
import com.kwvanderlinde.discordant.core.messages.scopes.DiscordUserScope;
import com.kwvanderlinde.discordant.core.messages.scopes.NilScope;
import com.kwvanderlinde.discordant.core.messages.scopes.NotificationStateScope;
import com.kwvanderlinde.discordant.core.messages.scopes.PendingVerificationScope;
import com.kwvanderlinde.discordant.core.messages.scopes.PlayerScope;
import com.kwvanderlinde.discordant.core.messages.scopes.ProfileScope;
import com.kwvanderlinde.discordant.core.messages.scopes.ServerScope;
import com.kwvanderlinde.discordant.core.modinterfaces.Integration;
import com.kwvanderlinde.discordant.core.modinterfaces.Player;
import com.kwvanderlinde.discordant.core.modinterfaces.Profile;
import com.kwvanderlinde.discordant.core.modinterfaces.Server;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
        catch (ConfigurationValidationFailed e) {
            logger.error("Invalid discordant configuration: {}", e.getMessage());
            return null;
        }
        catch (ModLoadFailed e) {
            logger.error("Failed to load Discordant mod: {}", e.getMessage(), e);
            return null;
        }
    }

    private @Nullable Server server;

    private ServerCache serverCache;
    private DiscordApi discordApi = new NullDiscordApi();
    private DiscordantConfig config;
    private LinkedProfileRepository linkedProfileRepository = new NullLinkedProfileRepository();
    private DiscordantAppender logAppender;

    private final Set<UUID> knownPlayerIds = new HashSet<>();
    private final HashMap<String, String> linkedPlayersByDiscordId = new HashMap<>();
    private final HashMap<UUID, VerificationData> pendingLinkVerification = new HashMap<>();
    private long currentTime = System.currentTimeMillis();
    private String botName;
    private final Pattern mentionPattern = Pattern.compile("(?<=@).+?(?=@|$|\\s)");
    private final Random r = new Random();

    public Discordant(Integration minecraftIntegration) throws ModLoadFailed, ConfigurationValidationFailed {
        final var configRoot = minecraftIntegration.getConfigRoot().resolve("discordant");

        this.serverCache = new FileBackedServerCache(configRoot.resolve("cache"));

        // The crucial bits are whether we can load our configuration and whether JDA can
        // initialize, so do those next to one another. Without those there is no hope.
        try {
            final var manager = new ConfigManager(configRoot);
            manager.ensureConfigStructure();
            config = manager.readDiscordLinkSettings();

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

            discordApi = new JdaDiscordApi(this, serverCache);

            discordApi.addListener(new DiscordantMessageListener(
                    this,
                    discordApi
            ));
        }
        catch (ConfigurationValidationFailed e) {
            throw e;
        }
        catch (Exception e) {
            throw new ModLoadFailed(e.getMessage(), e);
        }

        linkedProfileRepository = new WriteThroughLinkedProfileRepository(
                new HashTableLinkedProfileRepository(),
                new ConfigProfileRepository(configRoot.resolve("linked-profiles"))
        );
        botName = discordApi.getBotName();

        logAppender = new DiscordantAppender(Level.INFO, discordApi);
        ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).addAppender(logAppender);

        minecraftIntegration.enableCommands(config.linking.enabled && !config.linking.required);

        minecraftIntegration.events().onServerStarted((server) -> {
            Discordant.this.server = server;

            // TODO Attach server icon as a thumbnail or image if possible.
            final var message = config.discord.messages.serverStart
                    .instantiate(serverScope(server));
            discordApi.sendEmbed(buildMessageEmbed(message).build());

            // Update the channel topic.
            final var topic = config.discord.topics.channelTopic
                    .instantiate(serverScope(server));
            if (topic.description != null) {
                discordApi.setTopic(topic.description);
            }
        });
        minecraftIntegration.events().onServerStopping((server) -> {
            {
                // Update the channel topic.
                final var topic = config.discord.topics.shutdownTopic
                        .instantiate(serverScope(server));
                if (topic.description != null) {
                    discordApi.setTopic(topic.description);
                }
            }
            {
                final var message = config.discord.messages.serverStop
                        .instantiate(serverScope(server));
                discordApi.sendEmbed(buildMessageEmbed(message).build());
            }
        });
        minecraftIntegration.events().onServerStopped((server) -> {
            ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).removeAppender(logAppender);
            discordApi.close();
            discordApi = new NullDiscordApi();
        });
        minecraftIntegration.events().onTickStart((server) -> {
            currentTime = System.currentTimeMillis();
        });
        minecraftIntegration.events().onTickEnd(server -> {
            int tickCount = server.getTickCount();
            // TODO Configurable times for setting topic and expiring pending players. Actually, we
            //  don't even need to expire pending players, except to avoid stale data.
            if (tickCount % 6000 == 0) {
                // Update the channel topic.
                final var topic = config.discord.topics.channelTopic
                        .instantiate(serverScope(server));
                if (topic.description != null) {
                    discordApi.setTopic(topic.description);
                }
            }
            if (tickCount % 1200 == 0) {
                // Remove any expired pending verifications.
                final var iterator = pendingLinkVerification.entrySet().iterator();
                while (iterator.hasNext()) {
                    final var e = iterator.next();
                    VerificationData data = e.getValue();
                    if (currentTime > data.validUntil()) {
                        iterator.remove();
                    }
                }
            }
        });

        minecraftIntegration.events().onPlayerSentMessage((player, chatMessage, plainTextCompositeMessage) -> {
            if (config.enableMentions) {
                chatMessage = parseDiscordMentions(chatMessage);
            }

            final var message = config.discord.messages.playerChat
                    .instantiate(new ChatScope(
                            // TODO Look up the linked profile and pass the corresponding discord user.
                            playerScope(player.profile(), null),
                            chatMessage
                    ));
            final var e = buildMessageEmbed(message);

            e.setAuthor(player.name(), null, getPlayerIconUrl(player.profile()));
            {
                // If linking is enabled, actually use the discord details as the author.
                final var guild = discordApi.getGuild();
                if (config.linking.enabled && guild != null) {
                    LinkedProfile linkedProfile = linkedProfileRepository.getByPlayerId(player.uuid());
                    if (linkedProfile != null) {
                        Member m = guild.getMemberById(linkedProfile.discordId());
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

            final var linkedProfile = linkedProfileRepository.getByPlayerId(profile.uuid());
            if (linkedProfile != null) {
                // Reverse map so we can look up profile by discord ID.
                // TODO Is it worth handling edge case that there are existing entries? Would
                //  not be correct for them to exist, but bugs or instability may cause it.
                linkedPlayersByDiscordId.put(linkedProfile.discordId(), profile.name());
            }
            else if (config.linking.required) {
                // Profile does not exist. So send the user a code to verify with.
                final var authCode = this.generateLinkCode(profile.uuid(), profile.name());
                final var message = config.minecraft.messages.verificationDisconnect
                        .instantiate(new PendingVerificationScope(serverScope(server), authCode));
                reject.withReason(message);
            }

        });
        minecraftIntegration.events().onPlayerJoin((player) -> {
            knownPlayerIds.add(player.uuid());

            // TODO Look up the linked profile and pass the corresponding discord user.
            final var message = config.discord.messages.playerJoin
                    .instantiate(playerScope(player.profile(), null));

            discordApi.sendEmbed(buildMessageEmbed(message).build());
        });
        minecraftIntegration.events().onPlayerDisconnect(player -> {
            if (!knownPlayerIds.contains(player.uuid())) {
                return;
            }

            // TODO Look up the linked profile and pass the corresponding discord user.
            final var message = config.discord.messages.playerLeave
                    .instantiate(playerScope(player.profile(), null));
            discordApi.sendEmbed(buildMessageEmbed(message).build());

            knownPlayerIds.remove(player.uuid());
            // Remove in-memory profile linking. Will be reloaded next login if enabled.
            final var profile = linkedProfileRepository.getByPlayerId(player.uuid());
            if (profile != null) {
                linkedPlayersByDiscordId.remove(profile.discordId());
            }
        });
        minecraftIntegration.events().onPlayerDeath((player, deathMessage) -> {
            final var message = config.discord.messages.playerDeath
                    .instantiate(new DeathScope(
                            playerScope(player.profile(), null),
                            deathMessage
                    ));
            discordApi.sendEmbed(buildMessageEmbed(message).build());
        });
        minecraftIntegration.events().onPlayerAdvancement((player, advancement) -> {
            // TODO Look up the linked profile and pass the corresponding discord user.
            final var message = config.discord.messages.playerAdvancement
                    .instantiate(new AdvancementScope(
                            playerScope(player.profile(), null),
                            advancement.name(),
                            advancement.title(),
                            advancement.description()
                    ));
            discordApi.sendEmbed(buildMessageEmbed(message).build());
        });

        final var commandHandlers = minecraftIntegration.commandsHandlers();
        commandHandlers.link = (player, respondWith) -> {
            // TODO If already linked, tell the user instead of generating a new code.
            final var authCode = generateLinkCode(player.uuid(), player.name());
            final var scope = new PendingVerificationScope(serverScope(getServer()), authCode);
            respondWith.success(config.minecraft.messages.commandLinkMsg.instantiate(scope));
        };
        commandHandlers.unlink = (player, respondWith) -> {
            final var wasDeleted = removeLinkedProfile(player.uuid());
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

    public @Nullable Server getServer() {
        return server;
    }

    public String getBotName() {
        return botName;
    }

    public DiscordantConfig getConfig() {
        return config;
    }

    public @Nullable String getLinkedPlayerNameForDiscordId(String discordId) {
        return linkedPlayersByDiscordId.get(discordId);
    }

    public String generateLinkCode(UUID uuid, String name) {
        if (pendingLinkVerification.containsKey(uuid)) {
            return pendingLinkVerification.get(uuid).token();
        }

        final var authCode = r.nextInt(100_000, 1_000_000);
        final var data = new VerificationData(name, uuid, authCode, currentTime + config.linking.pendingTimeout);
        pendingLinkVerification.put(uuid, data);

        return data.token();
    }

    public boolean verifyLinkedProfile(final MessageChannel channelToRespondIn, final User author, final String verificationToken) {
        final var parts = verificationToken.split("\\|", 2);
        if (parts.length != 2) {
            logger.warn("Verification attempt with invalid token: {}", verificationToken);
            return false;
        }

        final UUID uuid;
        try {
            uuid = UUID.fromString(parts[0]);
        }
        catch (IllegalArgumentException e) {
            logger.warn("Verification attempt with invalid UUID: {}", parts[0]);
            return false;
        }

        final var verificationCode = parts[1];
        if (verificationCode.length() != 6 || !verificationCode.matches("[0-9]+")) {
            logger.warn("Verification attempt with invalid authentication code: {}", verificationCode);
            return false;
        }

        final var code = Integer.parseInt(verificationCode);
        final var data = pendingLinkVerification.get(uuid);
        if (data == null) {
            logger.warn("Verification attempt for player with no pending verification: {}", uuid);
            return false;
        }
        if (data.code() != code) {
            logger.warn("Verification attempt for player with incorrect code: {}, {}", uuid, code);
            return false;
        }

        final var existingProfile = linkedProfileRepository.getByPlayerId(uuid);
        if (existingProfile == null) {
            // Profile entry does not exist yet. Create it.
            final var newLinkedProfile = new LinkedProfile(data.name(), uuid, author.getId());
            linkedProfileRepository.put(newLinkedProfile);
            pendingLinkVerification.remove(uuid);
            if (!config.linking.required) {
                // TODO Player lookup should be unnecessary since player.name() should be the same
                //  as data.name(). But also why do we need to store the name?
                final var player = getServer().getPlayer(uuid);
                if (player != null) {
                    linkedPlayersByDiscordId.put(newLinkedProfile.discordId(), player.name());
                }
            }
            final var profile = new Profile() {
                @Override
                public UUID uuid() {
                    return newLinkedProfile.uuid();
                }

                @Override
                public String name() {
                    return newLinkedProfile.name();
                }
            };
            final var message = config.discord.messages.successfulVerification
                    .instantiate(playerScope(profile, author));
            discordApi.sendEmbed(channelToRespondIn, buildMessageEmbed(message).build());

            final var logMessage = String.format("Successfully linked discord account %s to minecraft account %s (%s)",
                                                 newLinkedProfile.discordId(),
                                                 newLinkedProfile.name(),
                                                 newLinkedProfile.uuid().toString());
            logger.info(logMessage);
            return true;
        }
        else {
            // Profile entry already exists. Tell that to the command issuer.
            final var guild = discordApi.getGuild();
            if (guild != null) {
                Member m = guild.getMemberById(existingProfile.discordId());
                final var profile = new Profile() {
                    @Override
                    public UUID uuid() {
                        return existingProfile.uuid();
                    }

                    @Override
                    public String name() {
                        return existingProfile.name();
                    }
                };
                final var message = config.discord.messages.alreadyLinked
                        .instantiate(playerScope(profile, m == null ? null : m.getUser()));
                discordApi.sendEmbed(channelToRespondIn, buildMessageEmbed(message).build());
                pendingLinkVerification.remove(uuid);
            }
            return false;
        }
    }

    public boolean removeLinkedProfile(UUID uuid) {
        LinkedProfile profile = linkedProfileRepository.getByPlayerId(uuid);
        if (profile != null) {
            linkedPlayersByDiscordId.remove(profile.discordId());
            linkedProfileRepository.delete(profile);
            return true;
        }
        return false;
    }

    public static EmbedBuilder buildMessageEmbed(DiscordMessageConfig<NilScope> config) {
        final var e = new EmbedBuilder();
        if (config.title != null) {
            e.setTitle(config.title);
        }
        if (config.color != null) {
            e.setColor(config.color.getRGB());
        }
        if (config.image != null) {
            e.setImage(config.image);
        }
        if (config.thumbnail != null) {
            e.setThumbnail(config.thumbnail);
        }
        if (config.description != null) {
            e.setDescription(config.description);
        }
        return e;
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

    public ServerScope serverScope(Server server) {
        return new ServerScope(
                config.minecraft.serverName,
                botName,
                server.motd(),
                server.getPlayerCount(),
                server.getMaxPlayers(),
                server.getAllPlayers().map(Player::name).toList(),
                currentTime
        );
    }

    private ProfileScope profileScope(Profile profile) {
        return new ProfileScope(
                serverScope(getServer()),
                profile.uuid(),
                profile.name()
        );
    }

    private PlayerScope playerScope(Profile profile, User user) {
        return new PlayerScope(
                profileScope(profile),
                (user == null)
                        ? new DiscordUserScope("", "", "")
                        : new DiscordUserScope(user.getId(), user.getName(), user.getAsTag()),
                getPlayerIconUrl(profile),
                getAvatarUrls(profile));
    }

    private String getPlayerIconUrl(Profile profile) {
        final var scopeValues = profileScope(profile).values();
        return ScopeUtil.instantiate(config.playerIconUrl, scopeValues)
                        .reduce(PlainTextRenderer.instance());
    }

    private Map<String, String> getAvatarUrls(Profile profile) {
        final var result = new HashMap<String, String>();
        final var scopeValues = profileScope(profile).values();
        for (final var entry : config.avatarUrls.entrySet()) {
            final var url = ScopeUtil.instantiate(entry.getValue(), scopeValues)
                                     .reduce(PlainTextRenderer.instance());
            result.put(entry.getKey(), url);
        }
        return result;
    }
}
