package com.kwvanderlinde.discordant.core.messages.scopes;

import com.kwvanderlinde.discordant.core.messages.MessageTemplate;
import com.kwvanderlinde.discordant.core.messages.ScopeUtil;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;
import com.kwvanderlinde.discordant.core.modinterfaces.Player;
import net.dv8tion.jda.api.entities.User;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public record PlayerScope(Player player, @Nullable User discordUser) implements Scope<PlayerScope> {
    public static MessageTemplate<PlayerScope> parse(String string) {
        return ScopeUtil.parse(string, List.of("uuid", "username", "discordid", "discordname", "discordtag"));
    }

    @Override
    public Map<String, SemanticMessage.Part> values() {
        return Map.of(
                "uuid", new SemanticMessage.Part.Literal(player.uuid().toString()),
                "username", new SemanticMessage.Part.Literal(player.name()),
                "discordid", discordUser == null ? new SemanticMessage.Part.Nil() : new SemanticMessage.Part.Literal(discordUser.getId()),
                "discordname", discordUser == null ? new SemanticMessage.Part.Nil() : new SemanticMessage.Part.Literal(discordUser.getName()),
                "discordtag", discordUser == null ? new SemanticMessage.Part.Nil() : new SemanticMessage.Part.Literal(discordUser.getAsTag())
        );
    }
}
