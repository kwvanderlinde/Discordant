package com.kwvanderlinde.discordant.core.messages.scopes;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import java.util.List;
import java.util.Map;

public record NilScope() implements Scope<NilScope> {
    public static List<String> parameters() {
        return List.of();
    }

    @Override
    public Map<String, SemanticMessage.Part> values() {
        return Map.of();
    }
}
