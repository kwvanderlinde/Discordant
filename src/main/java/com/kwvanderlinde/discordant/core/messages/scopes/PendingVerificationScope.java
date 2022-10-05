package com.kwvanderlinde.discordant.core.messages.scopes;

import com.kwvanderlinde.discordant.core.messages.MessageTemplate;
import com.kwvanderlinde.discordant.core.messages.ScopeUtil;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import java.util.List;
import java.util.Map;

public record PendingVerificationScope(String code, String botName) implements Scope<PendingVerificationScope> {
    public static MessageTemplate<PendingVerificationScope> parse(String string) {
        return ScopeUtil.parse(string, List.of("code", "botname"));
    }

    @Override
    public Map<String, SemanticMessage.Part> values() {
        return Map.of(
                "botname", new SemanticMessage.Part.BotName(this.botName()),
                "code", new SemanticMessage.Part.VerificationCode(this.code())
        );
    }
}
