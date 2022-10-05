package com.kwvanderlinde.discordant.core.messages.scopes;

import com.kwvanderlinde.discordant.core.messages.MessageTemplate;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import java.util.Map;

public interface Scope<T extends Scope<T>> {
    Map<String, SemanticMessage.Part> values();

    default SemanticMessage instantiate(MessageTemplate<T> template) {
        final var message = new SemanticMessage();

        final var values = values();
        for (final var part : template.parts()) {
            if (part instanceof MessageTemplate.LiteralPart literalPart) {
                message.appendLiteral(literalPart.text());
            }
            else if (part instanceof MessageTemplate.ParameterPart parameterPart) {
                final var value = values.get(parameterPart.name());
                // Skip any unknown parameters.
                if (value != null) {
                    message.append(value);
                }
            }
            else {
                assert false;
            }
        }

        return message;
    }
}
