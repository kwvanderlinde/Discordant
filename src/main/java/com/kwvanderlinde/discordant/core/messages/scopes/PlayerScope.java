package com.kwvanderlinde.discordant.core.messages.scopes;

import com.google.common.collect.ImmutableMap;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import javax.annotation.Nonnull;
import java.util.Map;

public record PlayerScope(ProfileScope profileScope, String discordId, String discordName, String discordTag, String iconUrl, Map<String, String> avatarUrls)
        implements DerivedScope<ProfileScope> {

    @Override
    public @Nonnull ProfileScope base() {
        return profileScope;
    }

    @Override
    public @Nonnull Map<String, SemanticMessage.Part> notInheritedValues() {
        final var result =
                ImmutableMap.<String, SemanticMessage.Part> builder()
                            .putAll(Map.of(
                                    "player.discordId", SemanticMessage.literal(discordId),
                                    "player.discordName", SemanticMessage.literal(discordName),
                                    "player.discordTag", SemanticMessage.literal(discordTag),
                                    "player.iconUrl", SemanticMessage.literal(iconUrl)));

        for (final var entry : avatarUrls.entrySet()) {
            result.put("player.avatarUrls|" + entry.getKey(), SemanticMessage.literal(entry.getValue()));
        }

        return result.build();
    }
}
