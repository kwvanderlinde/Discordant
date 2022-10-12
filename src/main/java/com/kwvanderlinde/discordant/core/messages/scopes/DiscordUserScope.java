package com.kwvanderlinde.discordant.core.messages.scopes;

import com.google.common.collect.ImmutableMap;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import javax.annotation.Nonnull;

public record DiscordUserScope(String userId, String userName, String userTag) implements Scope {
    @Override
    public void addValuesTo(@Nonnull ImmutableMap.Builder<String, SemanticMessage.Part> builder) {
        builder.put("player.discordId", SemanticMessage.literal(userId));
        builder.put("player.discordName", SemanticMessage.literal(userName));
        builder.put("player.discordTag", SemanticMessage.literal(userTag));
    }
}
