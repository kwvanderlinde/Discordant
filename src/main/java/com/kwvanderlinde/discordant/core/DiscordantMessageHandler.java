package com.kwvanderlinde.discordant.core;

import com.kwvanderlinde.discordant.core.config.DiscordantConfig;
import com.kwvanderlinde.discordant.core.discord.api.DiscordApi;
import com.kwvanderlinde.discordant.core.discord.api.MessageHandler;
import com.kwvanderlinde.discordant.core.linkedprofiles.LinkedProfileManager;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;
import com.kwvanderlinde.discordant.core.modinterfaces.Profile;
import com.kwvanderlinde.discordant.core.modinterfaces.Server;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DiscordantMessageHandler implements MessageHandler, ReloadableComponent {
    private static final Logger logger = LogManager.getLogger(DiscordantMessageHandler.class);
    private final Pattern pattern = Pattern.compile("(?<=!\\+).+?(?=!\\+|$|\\s)");
    private final Pattern pattern2 = Pattern.compile("(?<=<@).+?(?=>)");

    private DiscordantConfig config;
    private final LinkedProfileManager linkedProfileManager;
    private final ScopeFactory scopeFactory;
    private final EmbedFactory embedFactory;
    private final Server server;
    private final DiscordApi discordApi;

    public DiscordantMessageHandler(@Nonnull DiscordantConfig config,
                                    @Nonnull LinkedProfileManager linkedProfileManager,
                                    @Nonnull ScopeFactory scopeFactory,
                                    @Nonnull EmbedFactory embedFactory,
                                    @Nonnull Server server,
                                    @Nonnull DiscordApi discordApi) {
        this.config = config;
        this.linkedProfileManager = linkedProfileManager;
        this.scopeFactory = scopeFactory;
        this.embedFactory = embedFactory;
        this.server = server;
        this.discordApi = discordApi;
    }

    @Override
    public void reload(DiscordantConfig newConfig) {
        this.config = newConfig;
    }

    @Override
    public void onChatInput(MessageReceivedEvent e, String message) {
        if (!message.isEmpty()) {
            if (!message.startsWith("!@") && message.startsWith("!")) {
                handleCommandInput(e, message.substring(1));
            }
            else {
                handleChatMessage(e, message);
            }
        }
    }

    @Override
    public void onConsoleInput(MessageReceivedEvent e, String message) {
        logger.info("Discord user " + e.getAuthor().getName() + " running command " + message);
        server.runCommand(message);
    }

    @Override
    public void onBotPmInput(MessageReceivedEvent e, String message) {
        if (!config.linking.enabled) {
            return;
        }

        final var result = linkedProfileManager.verifyLinkedProfile(e.getChannel(), e.getAuthor(), message);
        if (result instanceof LinkedProfileManager.InvalidToken invalidToken) {
            logger.warn("Verification attempt with invalid token: {}", invalidToken.token());
        }
        else if (result instanceof LinkedProfileManager.InvalidUuid invalidUuid) {
            logger.warn("Verification attempt with invalid UUID: {}", invalidUuid.uuid());
        }
        else if (result instanceof LinkedProfileManager.InvalidCode invalidCode) {
            logger.warn("Verification attempt with invalid authentication code: {}", invalidCode.code());
        }
        else if (result instanceof LinkedProfileManager.NoPendingVerification noPendingVerification) {
            logger.warn("Verification attempt for player with no pending verification: {}", noPendingVerification.uuid());
        }
        else if (result instanceof LinkedProfileManager.IncorrectCode incorrectCode) {
            logger.warn("Verification attempt for player with incorrect code: {}, {}", incorrectCode.uuid(), incorrectCode.code());
        }
        else if (result instanceof LinkedProfileManager.AlreadyLinked alreadyLinked) {
            // Profile entry already exists. Tell that to the command issuer.
            logger.warn("Verification attempt when already linked: {}", message);
            final var existingProfile = alreadyLinked.existingProfile();
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
                final var response = config.discord.messages.alreadyLinked
                        .instantiate(scopeFactory.playerScope(profile, server, m == null ? null : m.getUser()));
                discordApi.sendEmbed(e.getChannel(), embedFactory.embedBuilder(response).build());
            }
        }
        else if (result instanceof LinkedProfileManager.SuccessfulLink successfulLink) {
            // Profile entry does not exist yet. Create it.
            final var newLinkedProfile = successfulLink.newProfile();
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
            final var response = config.discord.messages.successfulVerification
                    .instantiate(scopeFactory.playerScope(profile, server, e.getAuthor()));
            discordApi.sendEmbed(e.getChannel(), embedFactory.embedBuilder(response).build());

            logger.info("Successfully linked discord account {} to minecraft account {} ({})",
                        newLinkedProfile.discordId(),
                        newLinkedProfile.name(),
                        newLinkedProfile.uuid().toString());
        }
    }


    private void handleCommandInput(MessageReceivedEvent event, String command) {
        if (command.startsWith("list")) {
            final var players = server.getAllPlayers().toList();
            if (players.isEmpty()) {
                final var message = config.discord.messages.noPlayers
                        .instantiate(scopeFactory.serverScope(server));
                discordApi.sendEmbed(embedFactory.embedBuilder(message).build());
            }
            else {
                final var message = config.discord.messages.onlinePlayers
                        .instantiate(scopeFactory.serverScope(server));

                discordApi.sendEmbed(embedFactory.embedBuilder(message).build());
            }
        }
    }

    public void handleChatMessage(MessageReceivedEvent e, String message) {
        Member member = e.getMember();
        final Set<String> playerNamesToNotify = new HashSet<>();
        if (message.contains("!+")) {
            playerNamesToNotify.addAll(pattern.matcher(message).results().map(matchResult -> matchResult.group(0).toLowerCase()).collect(Collectors.toSet()));
        }
        if (message.contains("<@")) {
            List<String> ids = pattern2.matcher(message).results().map(matchResult -> matchResult.group(0)).toList();
            for (String s : ids) {
                if (s.startsWith("!")) {
                    s = s.substring(1);
                }
                String name = linkedProfileManager.getLinkedPlayerNameForDiscordId(s);
                if (config.linking.enabled && config.enableMentions && name != null) {
                    playerNamesToNotify.add(name.toLowerCase());
                    message = message.replaceAll("<(@.|@)" + s + ">", "@" + name);
                }
                else {
                    Member m = e.getGuild().getMemberById(s);
                    if (m != null) {
                        message = message.replaceAll("<(@.|@)" + s + ">", "@" + m.getEffectiveName());
                    }
                    else {
                        message = message.replaceAll("<(@.|@)" + s + ">", "@Unknown");
                    }
                }
            }
        }
        final var messageFinal = message;

        if (member != null) {
            String role = member.getRoles().isEmpty() ? "" : member.getRoles().get(0).getName();

            final var notifyMessage = new SemanticMessage()
                    .append(SemanticMessage.bot("[Discord] "))
                    .append(getChatComponent(role, member))
                    .append(" >> ");
            final var generalMessage = notifyMessage.copy()
                                                    .append(messageFinal);

            server.getAllPlayers().forEach(player -> {
                if (!playerNamesToNotify.contains(player.name().toLowerCase())) {
                    player.sendSystemMessage(generalMessage);
                }
                else {
                    final var name = player.name();
                    // TODO Use a semantic message part instead of format codes. And trim the !+.
                    final var replacement = messageFinal.replaceAll("(?i)!\\+" + name, "§a$0§r");
                    player.sendSystemMessage(notifyMessage.copy().append(replacement));
                    player.notifySound();
                }
            });
        }
    }

    private SemanticMessage.Part getChatComponent(String role, Member member) {
        return SemanticMessage.discordUser(
                member.getEffectiveName(),
                member.getUser().getAsTag(),
                role,
                member.getColorRaw()
        );
    }
}
