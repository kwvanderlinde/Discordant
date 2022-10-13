package com.kwvanderlinde.discordant.core.messages.scopes;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import javax.annotation.Nonnull;
import java.util.Map;

public record PendingVerificationScope(ServerScope serverScope, String code) implements SingleDerivedScope<ServerScope> {
    @Override
    public @Nonnull ServerScope base() {
        return serverScope;
    }

    @Override
    public @Nonnull Map<String, SemanticMessage.Part> notInheritedValues() {
        return Map.of(
                "verification.code", SemanticMessage.verificationCode(this.code())
        );
    }
}
