package com.kwvanderlinde.discordant.core.messages;

import java.util.stream.Stream;

public class SemanticMessageRenderer {
    public static String renderPlainText(Stream<SemanticMessage.Part> messageParts) {
        final var sb = new StringBuilder();
        messageParts.forEach(part -> {
            if (part instanceof SemanticMessage.Part.Nil) {
                // Do nothing.
            }
            else if (part instanceof SemanticMessage.Part.Literal literal) {
                sb.append(literal.text());
            }
            else if (part instanceof SemanticMessage.Part.VerificationCode verificationCode) {
                sb.append(verificationCode.code());
            }
            else if (part instanceof SemanticMessage.Part.DiscordUser discordUser) {
                sb.append(discordUser.name());
            }
            else if (part instanceof SemanticMessage.Part.BotName botName) {
                sb.append(botName.name());
            }
        });
        return sb.toString();
    }

    // TODO We might be able to create an embed formatter with colors and such.
}
