package com.kwvanderlinde.discordant.core.messages;

import com.kwvanderlinde.discordant.core.messages.scopes.Scope;

import java.util.List;

public record MessageTemplate<T extends Scope<T>>(List<Part> parts) {
    public sealed interface Part permits LiteralPart, ParameterPart {
    }

    public record LiteralPart(String text) implements Part {
    }

    public record ParameterPart(String name) implements Part {
    }

    public MessageTemplate(List<Part> parts) {
        this.parts = List.copyOf(parts);
    }
}
