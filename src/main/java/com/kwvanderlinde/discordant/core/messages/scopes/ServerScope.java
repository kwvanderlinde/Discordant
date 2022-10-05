package com.kwvanderlinde.discordant.core.messages.scopes;

import com.kwvanderlinde.discordant.core.messages.MessageTemplate;
import com.kwvanderlinde.discordant.core.messages.ScopeUtil;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ServerScope() implements Scope<ServerScope> {
    public static MessageTemplate<ServerScope> parse(String string) {
        return ScopeUtil.parse(string, List.of());
    }

    @Override
    public Map<String, SemanticMessage.Part> values() {
        return Map.of(
        );
    }
}
