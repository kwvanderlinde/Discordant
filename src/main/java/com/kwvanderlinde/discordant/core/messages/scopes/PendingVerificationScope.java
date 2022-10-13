package com.kwvanderlinde.discordant.core.messages.scopes;

import com.google.common.collect.ImmutableMap;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Map;

public record PendingVerificationScope(ServerScope serverScope, String code) implements Scope {
    @Override
    public void addValuesTo(@NotNull ImmutableMap.Builder<String, SemanticMessage.Part> builder) {
        serverScope.addValuesTo(builder);

        builder.put("verification.code", SemanticMessage.verificationCode(this.code()));
    }
}
