package com.kwvanderlinde.discordant.core.messages;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Represents a message using conventions appropriate for linked accounts.
 *
 * This is not capable of representing an arbitrary JSON-formatted Minecraft message.
 */
public class SemanticMessage {
    public static SemanticMessage.Part literal(String text) {
        return new Part(text, new PartTag.None());
    }

    public static SemanticMessage.Part bot(String botName) {
        return new Part(botName, new PartTag.Bot());
    }

    public static SemanticMessage.Part player(String name, UUID uuid) {
        return new Part(name, new PartTag.Player(uuid));
    }

    public static SemanticMessage.Part discordSender(@Nonnull String userName, @Nonnull String userTag, @Nullable String roleName, int roleColor) {
        return new Part(userName, new PartTag.DiscordSender(userTag, roleName, roleColor));
    }

    public static SemanticMessage.Part discordMention(@Nonnull String userName, @Nonnull String userTag, @Nullable String roleName, int roleColor) {
        return new Part(userName, new PartTag.DiscordMention(userTag, roleName, roleColor));
    }

    public static SemanticMessage.Part discordRoleMention(@Nonnull String name, int color) {
        return new Part(name, new PartTag.DiscordRoleMention(color));
    }

    public static SemanticMessage.Part discordChannelMention(@Nonnull String name, @Nonnull String jumpUrl) {
        return new Part(name, new PartTag.DiscordChannelMention(jumpUrl));
    }

    public static SemanticMessage.Part verificationCode(String code) {
        return new Part(code, new PartTag.VerificationCode());
    }


    private final List<Part> parts = new ArrayList<>();

    public SemanticMessage() {
    }

    public SemanticMessage copy() {
        final var result = new SemanticMessage();
        result.parts.addAll(this.parts);
        return result;
    }

    public Stream<Part> parts() {
        return parts.stream();
    }

    public <T> T reduce(SemanticMessageRenderer<T> reducer) {
        return reducer.render(this.parts());
    }

    public SemanticMessage append(String text) {
        parts.add(new Part(text, new PartTag.None()));
        return this;
    }

    public SemanticMessage append(String text, PartTag tag) {
        parts.add(new Part(text, tag));
        return this;
    }

    public SemanticMessage append(Part part) {
        parts.add(part);
        return this;
    }

    public record Part(String text, PartTag tag) {
    }

    public sealed interface PartTag permits PartTag.None, PartTag.Bot, PartTag.Player, PartTag.DiscordSender, PartTag.DiscordMention, PartTag.DiscordRoleMention, PartTag.DiscordChannelMention, PartTag.VerificationCode {
        record None() implements PartTag {}

        record Bot() implements PartTag {
            public int color() {
                // TODO Should be determined by Discordant, not the messaging library.
                return 6955481;
            }
        }

        record Player(UUID uuid) implements PartTag {}

        record DiscordSender(String userTag, String roleName, int roleColor) implements PartTag {}

        record DiscordMention(String userTag, String roleName, int roleColor) implements PartTag {}

        record DiscordRoleMention(int color) implements PartTag {}

        record DiscordChannelMention(String jumpUrl) implements PartTag {}

        record VerificationCode() implements PartTag {
            public int color() {
                // TODO Should be determined by Discordant, not the messaging library.
                return 5635925;
            }
        }
    }
}
