package com.kwvanderlinde.discordant.core.messages;

import java.util.Map;
import java.util.regex.Pattern;

public class ScopeUtil {
    private static final Pattern parameterPattern = Pattern.compile("\\{([a-zA-Z.|]*)}");

    public static SemanticMessage instantiate(String template, Map<String, SemanticMessage.Part> values) {
        final var result = new SemanticMessage();

        // TODO We can cache indices/parts per template string since there are a finite number of them.
        final var matcher = parameterPattern.matcher(template);
        var previousIndex = 0;
        while (matcher.find()) {
            final var fullStart = matcher.start(0);
            final var fullEnd = matcher.end(0);
            final var name = matcher.group(1);

            if (fullStart != previousIndex) {
                // There's stuff between the previous parameter and this one that needs to be added.
                result.append(template.substring(previousIndex, fullStart));
            }

            if (values.containsKey(name)) {
                result.append(values.get(name));
            }
            else {
                // Unknown parameter, leave the whole thing in there, curly braces and all.
                result.append(template.substring(fullStart, fullEnd));
            }

            previousIndex = fullEnd;
        }
        // There's stuff between the last parameter and the end that needs to be added.
        if (previousIndex != template.length()) {
            result.append(template.substring(previousIndex));
        }

        return result;
    }
}
