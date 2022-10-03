package com.kwvanderlinde.discordant.core.modinterfaces;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class SemanticMessage {
    private final List<Part> parts = new ArrayList<>();

    public SemanticMessage() {
    }

    public SemanticMessage append(Part part) {
        parts.add(part);
        return this;
    }

    public SemanticMessage appendLiteral(String text) {
        return append(new Part.Literal(text));
    }

    public SemanticMessage appendBotName(String name) {
        return append(new Part.BotName(name));
    }

    public SemanticMessage appendDiscordUser(String name, String roleName, String fullUserName, int roleColor) {
        return append(new Part.DiscordUser(name, roleName, fullUserName, roleColor));
    }

    public SemanticMessage appendVerificationCode(String code) {
        return append(new Part.VerificationCode(code));
    }

    public Stream<Part> parts() {
        return parts.stream();
    }

    /**
     * Represents a message using conventions appropriate for linked accounts.
     *
     * This is not capable of representing an arbitrary JSON-formatted Minecraft message.
     */
    public sealed interface Part permits Part.Literal, Part.BotName, Part.DiscordUser, Part.VerificationCode {
        record Literal(String text) implements Part {}

        record BotName(String name) implements Part {
            public int color() {
                return 6955481;
            }
        }

        record DiscordUser(String name, String roleName, String tag, int color) implements Part {}

        record VerificationCode(String code) implements Part {
            public int color() {
                return 5635925;
            }
        }
    }
}
