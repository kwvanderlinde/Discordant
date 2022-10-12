package com.kwvanderlinde.discordant.core.messages.scopes;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import javax.annotation.Nonnull;
import java.util.Map;

public record NotificationStateScope(boolean state) implements Scope {
    @Override
    public @Nonnull Map<String, SemanticMessage.Part> values() {
        return Map.of(
                "notifications.state", SemanticMessage.literal(String.valueOf(state)),
                "notifications.enablement", SemanticMessage.literal(state ? "enabled" : "disabled")
        );
    }
}
