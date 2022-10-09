package com.kwvanderlinde.discordant.core.messages.scopes;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import java.util.List;
import java.util.Map;

public record NotificationStateScope(boolean state) implements Scope<NotificationStateScope> {
    public static List<String> parameters() {
        return List.of("state");
    }

    @Override
    public Map<String, SemanticMessage.Part> values() {
        return Map.of(
                "state", SemanticMessage.literal(String.valueOf(state))
        );
    }
}
