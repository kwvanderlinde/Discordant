package com.kwvanderlinde.discordant.core.messages.scopes;

import com.google.common.collect.ImmutableMap;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import javax.annotation.Nonnull;

public record NotificationStateScope(boolean state) implements Scope {
    @Override
    public void addValuesTo(@Nonnull ImmutableMap.Builder<String, SemanticMessage.Part> builder) {
        builder.put("notifications.state", SemanticMessage.literal(String.valueOf(state)));
        builder.put("notifications.enablement", SemanticMessage.literal(state ? "enabled" : "disabled"));
    }
}
