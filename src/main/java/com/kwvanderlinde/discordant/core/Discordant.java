package com.kwvanderlinde.discordant.core;

import com.kwvanderlinde.discordant.core.config.ConfigManager;
import com.kwvanderlinde.discordant.core.config.DiscordMessageConfig;
import com.kwvanderlinde.discordant.core.discord.linkedprofiles.ConfigProfileRepository;
import com.kwvanderlinde.discordant.core.discord.api.DiscordApi;
import com.kwvanderlinde.discordant.core.discord.api.JdaDiscordApi;
import com.kwvanderlinde.discordant.core.discord.linkedprofiles.LinkedProfile;
import com.kwvanderlinde.discordant.core.discord.linkedprofiles.LinkedProfileRepository;
import com.kwvanderlinde.discordant.core.discord.api.NullDiscordApi;
import com.kwvanderlinde.discordant.core.discord.linkedprofiles.NullLinkedProfileRepository;
import com.kwvanderlinde.discordant.core.logging.DiscordantAppender;
import com.kwvanderlinde.discordant.core.config.DiscordantConfig;
import com.kwvanderlinde.discordant.core.discord.linkedprofiles.VerificationData;
import com.kwvanderlinde.discordant.core.messages.PlainTextRenderer;
import com.kwvanderlinde.discordant.core.messages.ScopeUtil;
import com.kwvanderlinde.discordant.core.messages.scopes.AdvancementScope;
import com.kwvanderlinde.discordant.core.messages.scopes.ChatScope;
import com.kwvanderlinde.discordant.core.messages.scopes.DeathScope;
import com.kwvanderlinde.discordant.core.messages.scopes.NilScope;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    private final Integration minecraftIntegration;

    private ServerCache serverCache;
    private DiscordApi discordApi = new NullDiscordApi();
    private DiscordantConfig config;
    private LinkedProfileRepository linkedProfileRepository = new NullLinkedProfileRepository();
    private DiscordantAppender logAppender;

    private final Set<UUID> knownPlayerIds = new HashSet<>();
    private final HashMap<UUID, LinkedProfile> linkedPlayers = new HashMap<>();
    private final HashMap<String, String> linkedPlayersByDiscordId = new HashMap<>();
    private final HashMap<Integer, VerificationData> pendingPlayers = new HashMap<>();
    private final HashMap<UUID, Integer> pendingPlayersUUID = new HashMap<>();
    private long currentTime = System.currentTimeMillis();
    private String botName;
    private final Pattern mentionPattern = Pattern.compile("(?<=@).+?(?=@|$|\\s)");
    private final Random r = new Random();

    // TODO Mod initializer also included language support. We should find a way to do that without a dependency on minecraft

    public Discordant(Integration minecraftIntegration) {
        this.minecraftIntegration = minecraftIntegration;

        final var configRoot = minecraftIntegration.getConfigRoot().resolve("discordant");

        this.serverCache = new FileBackedServerCache(configRoot.resolve("cache"));

        final var manager = new ConfigManager(configRoot);
        try {
            manager.ensureConfigStructure();
            config = manager.readDiscordLinkSettings();
        }
        // TODO Hard failure on some of these exceptions. And by "hard" I mean don't initialize
        //  most mod functionality.
        catch (IOException e) {
            e.printStackTrace();
        }
        linkedProfileRepository = new ConfigProfileRepository(manager.getConfigRoot().resolve("linked-profiles"));

        try {
            discordApi = new JdaDiscordApi(this, serverCache);

            logAppender = new DiscordantAppender(Level.INFO, discordApi);
            ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).addAppender(logAppender);

            botName = discordApi.getBotName();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        minecraftIntegration.enableCommands(config.enableAccountLinking && !config.forceLinking);

        minecraftIntegration.events().onServerStarted((server) -> {
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
                for (Map.Entry<Integer, VerificationData> e : pendingPlayers.entrySet()) {
                    VerificationData data = e.getValue();
                    if (currentTime > data.validUntil()) {
                        pendingPlayersUUID.remove(data.uuid());
                        pendingPlayers.remove(e.getKey());
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
                if (config.enableAccountLinking && guild != null) {
                    LinkedProfile linkedProfile = linkedPlayers.get(player.uuid());
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
            if (!config.enableAccountLinking) {
                // This handler is only for loading and checking linked accounts.
                return;
            }

            final var linkedProfile = linkedProfileRepository.get(profile.uuid());
            if (linkedProfile != null) {
                // Load the profile into memory so it is available for later operations.
                // TODO Is it worth handling edge case that there are existing entries? Would
                //  not be correct for them to exist, but bugs or instability may cause it.
                linkedPlayers.put(profile.uuid(), linkedProfile);
                linkedPlayersByDiscordId.put(linkedProfile.discordId(), profile.name());
            }

            if (config.forceLinking && linkedProfile == null) {
                // Profile does not exist. So send the user a code to verify with.
                final int authCode = this.generateLinkCode(profile.uuid(), profile.name());
                final var message = config.minecraft.messages.verificationDisconnect
                        .instantiate(new PendingVerificationScope(serverScope(server), String.valueOf(authCode)));
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
            LinkedProfile profile = linkedPlayers.get(player.uuid());
            if (profile != null) {
                linkedPlayersByDiscordId.remove(profile.discordId());
                linkedPlayers.remove(player.uuid());
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
    }

    public Server getServer() {
        return minecraftIntegration.getServer();
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

    public int generateLinkCode(UUID uuid, String name) {
        if (pendingPlayersUUID.containsKey(uuid)) {
            return pendingPlayersUUID.get(uuid);
        }

        int authCode = r.nextInt(100_000, 1_000_000);
        while (pendingPlayers.containsKey(authCode)) {
            // TODO Need a bailout for robustness.
            authCode = r.nextInt(100_000, 1_000_000);
        }
        pendingPlayers.put(authCode, new VerificationData(name, uuid, currentTime + 600_000));
        pendingPlayersUUID.put(uuid, authCode);

        return authCode;
    }

    public boolean verifyLinkedProfile(final MessageChannel channelToRespondIn, final User author, final String verificationCode) {
        if (verificationCode.length() != 6 || !verificationCode.matches("[0-9]+")) {
            return false;
        }

        final var code = Integer.parseInt(verificationCode);
        final var data = pendingPlayers.get(code);
        if (data == null) {
            return false;
        }

        final var id = data.uuid();

        // TODO Avoid filesystem access here, and rely on the repository as needed.
        if (!Files.exists(Paths.get(String.format("./config/discordant/linked-profiles/%s.json", id.toString())))) {
            // Profile entry does not exist yet. Create it.
            final var linkedProfile = new LinkedProfile(data.name(), id, author.getId());
            linkedProfileRepository.put(linkedProfile);
            pendingPlayersUUID.remove(id);
            pendingPlayers.remove(code);
            if (!config.forceLinking) {
                final var player = minecraftIntegration.getServer().getPlayer(id);
                if (player != null) {
                    linkedPlayers.put(player.uuid(), linkedProfile);
                    linkedPlayersByDiscordId.put(linkedProfile.discordId(), player.name());
                }
            }
            final var profile = new Profile() {
                @Override
                public UUID uuid() {
                    return linkedProfile.uuid();
                }

                @Override
                public String name() {
                    return linkedProfile.name();
                }
            };
            final var message = config.discord.messages.successfulVerification
                    .instantiate(playerScope(profile, author));
            discordApi.sendEmbed(channelToRespondIn, buildMessageEmbed(message).build());

            final var logMessage = String.format("Successfully linked discord account %s to minecraft account %s (%s)",
                                                 linkedProfile.discordId(),
                                                 linkedProfile.name(),
                                                 linkedProfile.uuid().toString());
            logger.info(logMessage);
            return true;
        }
        else {
            // Profile entry already exists. Tell that to the command issuer.
            LinkedProfile linkedProfile = linkedProfileRepository.get(id);
            final var guild = discordApi.getGuild();
            if (linkedProfile != null && guild != null) {
                Member m = guild.getMemberById(linkedProfile.discordId());
                final var profile = new Profile() {
                    @Override
                    public UUID uuid() {
                        return linkedProfile.uuid();
                    }

                    @Override
                    public String name() {
                        return linkedProfile.name();
                    }
                };
                final var message = config.discord.messages.alreadyLinked
                        .instantiate(playerScope(profile, m == null ? null : m.getUser()));
                discordApi.sendEmbed(channelToRespondIn, buildMessageEmbed(message).build());
                pendingPlayersUUID.remove(id);
                pendingPlayers.remove(code);
            }
            return false;
        }
    }

    public boolean removeLinkedProfile(UUID uuid) {
        LinkedProfile profile = linkedProfileRepository.get(uuid);
        if (profile != null) {
            linkedPlayersByDiscordId.remove(profile.discordId());
            linkedPlayers.remove(uuid);
            linkedProfileRepository.delete(uuid);
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
                (user == null) ? "" : user.getId(),
                (user == null) ? "" : user.getName(),
                (user == null) ? "" : user.getAsTag(),
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
