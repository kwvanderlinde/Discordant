package com.kwvanderlinde.discordant.core.messages.scopes;

import com.google.common.collect.ImmutableMap;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import javax.annotation.Nonnull;
import java.util.Map;

public record PlayerScope(ProfileScope profileScope, DiscordUserScope discordUserScope, String iconUrl, Map<String, String> avatarUrls) implements Scope {
    @Override
    public void addValuesTo(@Nonnull ImmutableMap.Builder<String, SemanticMessage.Part> builder) {
        profileScope.addValuesTo(builder);
        discordUserScope.addValuesTo(builder);

        builder.put("player.iconUrl", SemanticMessage.literal(iconUrl));
        for (final var entry : avatarUrls.entrySet()) {
            builder.put("player.avatarUrls|" + entry.getKey(), SemanticMessage.literal(entry.getValue()));
        }
    }
}
