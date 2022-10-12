package com.kwvanderlinde.discordant.core.messages.scopes;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;
import com.kwvanderlinde.discordant.core.modinterfaces.Profile;
import net.dv8tion.jda.api.entities.User;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record PlayerScope(Profile profile, @Nullable User discordUser, String iconUrl, Map<String, String> avatarUrls) implements Scope<PlayerScope> {
    public static List<String> parameters() {
        return List.of("uuid", "username", "player.uuid", "player.name", "discordid", "discordname", "discordtag", "player.iconUrl");
    }

    @Override
    public Map<String, SemanticMessage.Part> values() {
        final var result = new HashMap<String, SemanticMessage.Part>();
        result.putAll(Map.of(
                "uuid", SemanticMessage.literal(profile.uuid().toString()),
                "username", SemanticMessage.literal(profile.name()),
                "player.uuid", SemanticMessage.literal(profile.uuid().toString()),
                "player.name", SemanticMessage.literal(profile.name()),
                "discordid", discordUser == null ? SemanticMessage.literal("") : SemanticMessage.literal(discordUser.getId()),
                "discordname", discordUser == null ? SemanticMessage.literal("") : SemanticMessage.literal(discordUser.getName()),
                "discordtag", discordUser == null ? SemanticMessage.literal("") : SemanticMessage.literal(discordUser.getAsTag()),
                "player.iconUrl", SemanticMessage.literal(iconUrl)
        ));
        for (final var entry : avatarUrls.entrySet()) {
            result.put("player.avatarUrls|" + entry.getKey(), SemanticMessage.literal(entry.getValue()));
        }

        return result;
    }
}
