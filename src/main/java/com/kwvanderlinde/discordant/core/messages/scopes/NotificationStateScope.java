package com.kwvanderlinde.discordant.core.messages.scopes;

import com.kwvanderlinde.discordant.core.messages.MessageTemplate;
import com.kwvanderlinde.discordant.core.messages.ScopeUtil;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import java.util.List;
import java.util.Map;

public record NotificationStateScope(boolean state) implements Scope<NotificationStateScope> {
    public static MessageTemplate<NotificationStateScope> parse(String string) {
        return ScopeUtil.parse(string, List.of("state"));
    }

    @Override
    public Map<String, SemanticMessage.Part> values() {
        return Map.of(
                "state", new SemanticMessage.Part.Literal(String.valueOf(state))
        );
    }
}
