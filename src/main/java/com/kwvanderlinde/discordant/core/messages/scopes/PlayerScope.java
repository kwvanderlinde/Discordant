package com.kwvanderlinde.discordant.core.messages.scopes;

import com.google.common.collect.ImmutableMap;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record PlayerScope(ProfileScope profileScope, String discordId, String discordName, String discordTag, String iconUrl, Map<String, String> avatarUrls)
        implements Scope {
    @Override
    public void addValuesTo(@NotNull ImmutableMap.Builder<String, SemanticMessage.Part> builder) {
        profileScope.addValuesTo(builder);

        builder.put("player.discordId", SemanticMessage.literal(discordId));
        builder.put("player.discordName", SemanticMessage.literal(discordName));
        builder.put("player.discordTag", SemanticMessage.literal(discordTag));
        builder.put("player.iconUrl", SemanticMessage.literal(iconUrl));
        for (final var entry : avatarUrls.entrySet()) {
            builder.put("player.avatarUrls|" + entry.getKey(), SemanticMessage.literal(entry.getValue()));
        }
    }
}
