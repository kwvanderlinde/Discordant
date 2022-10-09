package com.kwvanderlinde.discordant.core.messages;

import java.util.stream.Stream;

@FunctionalInterface
public interface SemanticMessageRenderer<OutputType> {
    OutputType render(Stream<SemanticMessage.Part> messageParts);
}
