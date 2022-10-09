package com.kwvanderlinde.discordant.core.messages;

import java.util.ArrayList;
import java.util.List;
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

    public static SemanticMessage.Part verificationCode(String code) {
        return new Part(code, new PartTag.VerificationCode());
    }


    private final List<Part> parts = new ArrayList<>();

    public SemanticMessage() {
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

    public sealed interface PartTag permits PartTag.None, PartTag.Bot, PartTag.DiscordUser, PartTag.VerificationCode {
        record None() implements PartTag {}

        record Bot() implements PartTag {
            public int color() {
                // TODO Should be determined by Discordant, not the messaging library.
                return 6955481;
            }
        }

        record DiscordUser(String userTag, String roleName, int roleColor) implements PartTag {}

        record VerificationCode() implements PartTag {
            public int color() {
                // TODO Should be determined by Discordant, not the messaging library.
                return 5635925;
            }
        }
    }
}
