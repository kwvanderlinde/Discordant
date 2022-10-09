package com.kwvanderlinde.discordant.core.messages.scopes;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import java.util.Map;

public interface Scope<T extends Scope<T>> {
    Map<String, SemanticMessage.Part> values();
}
