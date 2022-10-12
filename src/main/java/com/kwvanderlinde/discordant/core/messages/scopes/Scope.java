package com.kwvanderlinde.discordant.core.messages.scopes;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import javax.annotation.Nonnull;
import java.util.Map;

public interface Scope {
    @Nonnull
    Map<String, SemanticMessage.Part> values();
}
