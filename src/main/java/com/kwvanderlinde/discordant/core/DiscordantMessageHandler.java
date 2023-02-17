package com.kwvanderlinde.discordant.core;

import com.kwvanderlinde.discordant.core.config.DiscordantConfig;
import com.kwvanderlinde.discordant.core.discord.api.DiscordApi;
import com.kwvanderlinde.discordant.core.discord.api.MessageHandler;
import com.kwvanderlinde.discordant.core.linkedprofiles.LinkedProfileManager;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;
import com.kwvanderlinde.discordant.core.modinterfaces.Profile;
import com.kwvanderlinde.discordant.core.modinterfaces.Server;
import com.kwvanderlinde.discordant.core.utils.StringUtils;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DiscordantMessageHandler implements MessageHandler, ReloadableComponent {
    private static final Logger logger = LogManager.getLogger(DiscordantMessageHandler.class);

    private DiscordantConfig config;
    private final LinkedProfileManager linkedProfileManager;
    private final ScopeFactory scopeFactory;
    private final EmbedFactory embedFactory;
    private final DiscordApi discordApi;
    private @Nullable Server server;

    public DiscordantMessageHandler(@Nonnull DiscordantConfig config,
                                    @Nonnull LinkedProfileManager linkedProfileManager,
                                    @Nonnull ScopeFactory scopeFactory,
                                    @Nonnull EmbedFactory embedFactory,
                                    @Nonnull DiscordApi discordApi) {
        this.config = config;
        this.linkedProfileManager = linkedProfileManager;
        this.scopeFactory = scopeFactory;
        this.embedFactory = embedFactory;
        this.discordApi = discordApi;
    }

    @Override
    public void reload(DiscordantConfig newConfig) {
        this.config = newConfig;
    }

    public void setServer(@Nonnull Server server) {
        this.server = server;
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

        final var result = linkedProfileManager.verifyLinkedProfile(e.getAuthor().getId(), message);
        if (result instanceof LinkedProfileManager.InvalidCode invalidCode) {
            logger.warn("Verification attempt with invalid authentication code: {}", invalidCode.code());
        }
        else if (result instanceof LinkedProfileManager.IncorrectCode incorrectCode) {
            logger.warn("Verification attempt for with incorrect code: {}", incorrectCode.code());
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
        // Map mentioned discord IDs to mentioned members.
        final var memberMentions = e.getMessage()
                                    .getMentions()
                                    .getMembers()
                                    .stream()
                                    .collect(Collectors.toMap(
                                            ISnowflake::getId,
                                            Function.identity()
                                    ));
        final var roleMentions = e.getMessage()
                                    .getMentions()
                                    .getRoles()
                                    .stream()
                                    .collect(Collectors.toMap(
                                            ISnowflake::getId,
                                            Function.identity()
                                    ));
        final var channelMentions = e.getMessage()
                                  .getMentions()
                                  .getChannels()
                                  .stream()
                                  .collect(Collectors.toMap(
                                          ISnowflake::getId,
                                          Function.identity()
                                  ));

        final Set<String> playerNamesToNotify = memberMentions.keySet()
                                                              .stream()
                                                              .map(linkedProfileManager::getLinkedPlayerNameForDiscordId)
                                                              .filter(Objects::nonNull)
                                                              .map(String::toLowerCase)
                                                              .collect(Collectors.toSet());

        // We need to replace discord mentions with corresponding usernames. If we find an
        // unlinked one, keep as the discord nickname.
        final var notifyMessage = new SemanticMessage();
        notifyMessage.append(SemanticMessage.bot("[Discord] "));
        if (member != null) {
            notifyMessage.append(SemanticMessage.discordSender(
                    member.getEffectiveName(),
                    member.getUser().getAsTag(),
                    member.getRoles().isEmpty() ? "" : member.getRoles().get(0).getName(),
                    member.getColorRaw()
            ));
        }
        notifyMessage.append(" says: ");

        final var fullPattern = Message.MentionType.USER.getPattern().pattern() + "|" + Message.MentionType.ROLE.getPattern().pattern() + "|" + Message.MentionType.CHANNEL.getPattern().pattern();
        // final var fullPattern = Message.MentionType.USER.getPattern().pattern()

        final var mentionPattern = Pattern.compile("<(@!?|@&|#)(\\d+)>");

        StringUtils.chunk(mentionPattern, message, new StringUtils.ChunkConsumer() {
            @Override
            public void onMatch(MatchResult matchResult) {
                // Convert the mentioned ID into a player name.
                final var discriminator = matchResult.group(1);
                final var id = matchResult.group(2);

                switch (discriminator) {
                    case "@", "@!" -> {
                        // Mentioned a member.
                        // The ID should always exist if things are working properly.
                        final var discordMember = memberMentions.get(id);
                        final var playerName = Objects.requireNonNullElse(
                                linkedProfileManager.getLinkedPlayerNameForDiscordId(id),
                                discordMember.getEffectiveName()
                        );
                        notifyMessage.append(SemanticMessage.discordMention(
                                playerName,
                                discordMember.getUser().getAsTag(),
                                discordMember.getRoles().isEmpty() ? "" : discordMember.getRoles().get(0).getName(),
                                discordMember.getColorRaw()
                        ));
                    }
                    case "@&" -> {
                        // Mentioned a role.
                        // The ID should always exist if things are working properly.
                        final var discordRole = roleMentions.get(id);
                        notifyMessage.append(SemanticMessage.discordRoleMention(
                                discordRole.getName(),
                                discordRole.getColorRaw()
                        ));
                    }
                    case "#" -> {
                        // Mentioned a channel.
                        // The ID should always exist if things are working properly.
                        final var discordChannel = channelMentions.get(id);
                        notifyMessage.append(SemanticMessage.discordChannelMention(
                                discordChannel.getName(),
                                discordChannel.getJumpUrl()
                        ));
                    }
                }
            }

            @Override
            public void onBetween(String contents) {
                notifyMessage.append(contents);
            }
        });

        server.getAllPlayers().forEach(player -> {
            player.sendSystemMessage(notifyMessage);

            if (playerNamesToNotify.contains(player.name().toLowerCase())) {
                player.notifySound();
            }
        });
    }
}
