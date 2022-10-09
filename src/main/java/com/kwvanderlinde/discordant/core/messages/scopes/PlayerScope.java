package com.kwvanderlinde.discordant.core.messages.scopes;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;
import com.kwvanderlinde.discordant.core.modinterfaces.Player;
import net.dv8tion.jda.api.entities.User;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public record PlayerScope(Player player, @Nullable User discordUser, String iconUrl) implements Scope<PlayerScope> {
    public static List<String> parameters() {
        return List.of("uuid", "username", "player.uuid", "player.name", "discordid", "discordname", "discordtag", "player.iconUrl");
    }

    @Override
    public Map<String, SemanticMessage.Part> values() {
        return Map.of(
                "uuid", SemanticMessage.literal(player.uuid().toString()),
                "username", SemanticMessage.literal(player.name()),
                "player.uuid", SemanticMessage.literal(player.uuid().toString()),
                "player.name", SemanticMessage.literal(player.name()),
                "discordid", discordUser == null ? SemanticMessage.literal("") : SemanticMessage.literal(discordUser.getId()),
                "discordname", discordUser == null ? SemanticMessage.literal("") : SemanticMessage.literal(discordUser.getName()),
                "discordtag", discordUser == null ? SemanticMessage.literal("") : SemanticMessage.literal(discordUser.getAsTag()),
                "player.iconUrl", SemanticMessage.literal(iconUrl)
        );
    }
}
