package com.kwvanderlinde.discordant.core.messages.scopes;

import com.google.common.collect.ImmutableMap;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import javax.annotation.Nonnull;

public record PendingVerificationScope(ServerScope serverScope, String code) implements Scope {
    @Override
    public void addValuesTo(@Nonnull ImmutableMap.Builder<String, SemanticMessage.Part> builder) {
        serverScope.addValuesTo(builder);

        builder.put("verification.code", SemanticMessage.verificationCode(this.code()));
    }
}
