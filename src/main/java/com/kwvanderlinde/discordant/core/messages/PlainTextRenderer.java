package com.kwvanderlinde.discordant.core.messages;

import java.util.stream.Stream;

public class PlainTextRenderer implements SemanticMessageRenderer<String> {
    public static SemanticMessageRenderer<String> instance() {
        return new PlainTextRenderer();
    }

    @Override
    public String render(Stream<SemanticMessage.Part> messageParts) {
        final var sb = new StringBuilder();
        messageParts.map(SemanticMessage.Part::text).forEach(sb::append);
        return sb.toString();
    }

    // TODO We might be able to create an embed formatter with colors and such.
}
