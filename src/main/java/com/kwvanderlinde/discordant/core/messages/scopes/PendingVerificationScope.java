package com.kwvanderlinde.discordant.core.messages.scopes;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import java.util.List;
import java.util.Map;

public record PendingVerificationScope(String code, String botName) implements Scope<PendingVerificationScope> {
    public static List<String> parameters() {
        return List.of("code", "botname");
    }

    @Override
    public Map<String, SemanticMessage.Part> values() {
        return Map.of(
                "botname", SemanticMessage.bot(this.botName()),
                "code", SemanticMessage.verificationCode(this.code())
        );
    }
}
