package com.kwvanderlinde.discordant.core.messages;

import com.kwvanderlinde.discordant.core.Discordant;
import com.kwvanderlinde.discordant.core.messages.scopes.Scope;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class ScopeUtil {
    public static <T extends Scope<T>> MessageTemplate<T> parse(String string, List<String> parameterNames) {
        final var matcher = Pattern.compile("\\{([a-zA-Z]*)}").matcher(string);
        var previousIndex = 0;

        Set<String> seenParameterNames = new HashSet<>();
        List<MessageTemplate.Part> parts = new ArrayList<>();
        while (matcher.find()) {
            final var fullStart = matcher.start(0);
            final var fullEnd = matcher.end(0);
            final var name = matcher.group(1);

            seenParameterNames.add(name);
            if (fullStart != previousIndex) {
                parts.add(new MessageTemplate.LiteralPart(string.substring(previousIndex, fullStart)));
            }
            parts.add(new MessageTemplate.ParameterPart(name));

            previousIndex = fullEnd;
        }
        if (previousIndex != string.length()) {
            parts.add(new MessageTemplate.LiteralPart(string.substring(previousIndex)));
        }

        if (!parameterNames.containsAll(seenParameterNames)) {
            final var unknownParameters = new HashSet<>(seenParameterNames);
            unknownParameters.removeAll(parameterNames);
            Discordant.logger.error("Found unknown parameter names while parsing: {}", unknownParameters);
        }

        return new MessageTemplate<>(parts);
    }
}
