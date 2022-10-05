package com.kwvanderlinde.discordant.core;

import com.kwvanderlinde.discordant.core.config.ConfigManager;
import com.kwvanderlinde.discordant.core.discord.linkedprofiles.ConfigProfileRepository;
import com.kwvanderlinde.discordant.core.discord.api.DiscordApi;
import com.kwvanderlinde.discordant.core.discord.api.JdaDiscordApi;
import com.kwvanderlinde.discordant.core.discord.linkedprofiles.LinkedProfile;
import com.kwvanderlinde.discordant.core.discord.linkedprofiles.LinkedProfileRepository;
import com.kwvanderlinde.discordant.core.discord.api.NullDiscordApi;
import com.kwvanderlinde.discordant.core.discord.linkedprofiles.NullLinkedProfileRepository;
import com.kwvanderlinde.discordant.core.logging.DiscordantAppender;
import com.kwvanderlinde.discordant.core.config.DiscordConfig;
import com.kwvanderlinde.discordant.core.discord.linkedprofiles.VerificationData;
import com.kwvanderlinde.discordant.core.modinterfaces.Integration;
import com.kwvanderlinde.discordant.core.modinterfaces.Player;
import com.kwvanderlinde.discordant.core.modinterfaces.SemanticMessage;
import kong.unirest.Unirest;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.minecraft.locale.Language;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.awt.Color;
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
    public static Discordant create(Integration integration) {
        return new Discordant(integration);
    }

    public static final Logger logger = LogManager.getLogger("Discordant");

    private final Integration minecraftIntegration;

    private ServerCache serverCache;
    private DiscordApi discordApi = new NullDiscordApi();
    private DiscordConfig config;
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
            config.setup();
        }
        // TODO Hard failure on some of these exceptions. And by "hard" I mean don't initialize
        //  most mod functionality.
        catch (IOException e) {
            e.printStackTrace();
        }

        linkedProfileRepository = new ConfigProfileRepository(manager.getConfigRoot().resolve("linked-profiles"));

        try {
            discordApi = new JdaDiscordApi(config, serverCache);

            logAppender = new DiscordantAppender(Level.INFO, discordApi);
            ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).addAppender(logAppender);

            botName = discordApi.getBotName();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        minecraftIntegration.enableBaseCommands();
        if (config.enableAccountLinking && !config.forceLinking) {
            minecraftIntegration.enableLinkingCommands();
        }

        minecraftIntegration.events().onServerStarted((server) -> {
            discordApi.postChatMessage(config.startupMsg);
        });
        minecraftIntegration.events().onServerStopping((server) -> {
            shutdown();
        });
        minecraftIntegration.events().onTickStart((server) -> {
            currentTime = System.currentTimeMillis();
        });
        minecraftIntegration.events().onTickEnd(server -> {
            int tickCount = server.getTickCount();
            // TODO Configurable times for setting topic and expiring pending players. Actually, we
            //  don't even need to expire pending players, except to avoid stale data.
            if (tickCount % 6000 == 0) {
                discordApi.setTopic(config.channelTopicMsg + server.getPlayerCount() + "/" + server.getMaxPlayers());
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

        minecraftIntegration.events().onPlayerSentMessage((player, message) -> {
            if (config.enableMentions) {
                message = parseDiscordMentions(message);
            }
            if (config.enableWebhook) {
                final var guild = discordApi.getGuild();
                if (config.enableAccountLinking && guild != null && config.useDiscordData) {
                    LinkedProfile profile = linkedPlayers.get(player.uuid());
                    if (profile != null) {
                        Member m = guild.getMemberById(profile.discordId());
                        if (m != null) {
                            discordApi.postWebHookMsg(message, m.getEffectiveName(), m.getEffectiveAvatarUrl());
                            return;
                        }
                    }
                }
                discordApi.postWebHookMsg(message, player.name(), getPlayerIconUrl(player));
            }
            else {
                discordApi.postChatMessage(message);
            }
        });

        minecraftIntegration.events().onPlayerJoinAttempt((player, reject) -> {
            if (!config.enableAccountLinking) {
                // This handler is only for loading and checking linked accounts.
                return;
            }

            final var profile = linkedProfileRepository.get(player.uuid());
            if (profile != null) {
                // Load the profile into memory so it is available for later operations.
                // TODO Is it worth handling edge case that there are existing entries? Would
                //  not be correct for them to exist, but bugs or instability may cause it.
                linkedPlayers.put(player.uuid(), profile);
                linkedPlayersByDiscordId.put(profile.discordId(), player.name());
            }

            if (config.forceLinking && profile == null) {
                // Profile does not exist. So send the user a code to verify with.
                final int authCode = this.generateLinkCode(player.uuid(), player.name());
                final var message = renderMessageTemplate(config.verificationDisconnect, Map.of(
                        "{botname}", new SemanticMessage.Part.BotName(botName),
                        "{code}", new SemanticMessage.Part.VerificationCode(String.valueOf(authCode))));

                reject.withReason(message);
            }

        });
        minecraftIntegration.events().onPlayerJoin((player) -> {
            knownPlayerIds.add(player.uuid());

            EmbedBuilder e = new EmbedBuilder();
            e.setAuthor(config.joinMessage.replaceAll("\\{username}", player.name()), null, getPlayerIconUrl(player));
            e.setColor(Color.GREEN);
            discordApi.sendEmbed(e.build());
        });
        minecraftIntegration.events().onPlayerDisconnect(player -> {
            if (!knownPlayerIds.contains(player.uuid())) {
                return;
            }

            EmbedBuilder e = new EmbedBuilder();
            e.setAuthor(config.disconnectMessage.replaceAll("\\{username}", player.name()), null, getPlayerIconUrl(player));
            e.setColor(Color.RED);
            discordApi.sendEmbed(e.build());

            knownPlayerIds.remove(player.uuid());

            // Remove in-memory profile linking. Will be reloaded next login if enabled.
            LinkedProfile profile = linkedPlayers.get(player.uuid());
            if (profile != null) {
                linkedPlayersByDiscordId.remove(profile.discordId());
                linkedPlayers.remove(player.uuid());
            }
        });
        minecraftIntegration.events().onPlayerDeath((player, message) -> {
            EmbedBuilder e = new EmbedBuilder();
            e.setAuthor(message, null, getPlayerIconUrl(player));
            e.setColor(Color.RED);
            discordApi.sendEmbed(e.build());
        });
        minecraftIntegration.events().onPlayerAdvancement((player, advancement) -> {
            EmbedBuilder e = new EmbedBuilder();
            e.setAuthor(String.format(Language.getInstance().getOrDefault("chat.type.advancement." + advancement.name()), player.name(), advancement.title()), null, getPlayerIconUrl(player));
            if (config.appendAdvancementDescription) {
                e.setDescription(String.format("** %s **", advancement.description()));
            }
            e.setColor(12524269);
            discordApi.sendEmbed(e.build());
        });
    }

    public String getBotName() {
        return botName;
    }

    public DiscordConfig getConfig() {
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
            LinkedProfile profile = new LinkedProfile(data.name(), id, author.getId());
            linkedProfileRepository.put(profile);
            pendingPlayersUUID.remove(id);
            pendingPlayers.remove(code);
            if (!config.forceLinking) {
                final var player = minecraftIntegration.getServer().getPlayer(id);
                if (player != null) {
                    linkedPlayers.put(player.uuid(), profile);
                    linkedPlayersByDiscordId.put(profile.discordId(), player.name());
                }
            }
            channelToRespondIn.sendMessage(config.successfulVerificationMsg
                                               .replaceAll("\\{username}", data.name()).replaceAll("\\{uuid}", id.toString())).queue();
            logger.info(config.successLinkDiscordMsg.replaceAll("\\{username}", data.name()).replaceAll("\\{discordname}", author.getName()));
            return true;
        }
        else {
            // Profile entry already exists. Tell that to the command issuer.
            LinkedProfile profile = linkedProfileRepository.get(id);
            final var guild = discordApi.getGuild();
            if (profile != null && guild != null) {
                Member m = guild.getMemberById(profile.discordId());
                String discordName = m != null ? m.getEffectiveName() : "Unknown user";
                channelToRespondIn.sendMessage(config.alreadyLinked.replaceAll("\\{username}", profile.name()).replaceAll("\\{discordname}", discordName)).queue();
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

    private String getPlayerIconUrl(Player player) {
        return config.playerHeadsUrl.replaceAll("\\{username}", player.name()).replaceAll("\\{uuid}", player.uuid().toString());
    }

    private void shutdown() {
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

    // TODO Instead of filling a string, parse the string and render the AST.
    private SemanticMessage renderMessageTemplate(String template, Map<String, SemanticMessage.Part> variables) {
        final var message = new SemanticMessage();

        var previousIndex = 0;
        while (true) {
            var minIndex = Integer.MAX_VALUE;
            int endIndex = template.length();
            SemanticMessage.Part minPart = null;
            for (final var entry : variables.entrySet()) {
                var index = template.indexOf(entry.getKey(), previousIndex);
                if (index >= 0 && index < minIndex) {
                    minIndex = index;
                    minPart = entry.getValue();
                    endIndex = index + entry.getKey().length();
                }
            }

            if (minIndex == Integer.MAX_VALUE) {
                assert minPart == null;
                final var remaining = template.substring(previousIndex);
                if (!remaining.isEmpty()) {
                    message.appendLiteral(remaining);
                }
                break;
            }
            if (minIndex != previousIndex) {
                message.appendLiteral(template.substring(previousIndex, minIndex));
            }

            assert minPart != null;
            message.append(minPart);
            previousIndex = endIndex;
        }

        return message;
    }
}
