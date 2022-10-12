package com.kwvanderlinde.discordant.core.messages;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class ScopeUtil {
    private static final Pattern parameterPattern = Pattern.compile("\\{([a-zA-Z.|]*)}");

    public static SemanticMessage instantiate(String template, Map<String, SemanticMessage.Part> values) {
        final var result = new SemanticMessage();

        // TODO We can cache indices/parts per template string since there are a finite number of them.
        final var matcher = parameterPattern.matcher(template);
        var previousIndex = 0;
        Set<String> seenParameterNames = new HashSet<>();
        while (matcher.find()) {
            final var fullStart = matcher.start(0);
            final var fullEnd = matcher.end(0);
            final var name = matcher.group(1);

            if (!values.containsKey(name)) {
                // Unknown parameter, leave it in there.
                result.append(template.substring(previousIndex, fullEnd));
                continue;
            }

            if (fullStart != previousIndex) {
                // There's stuff between the previous parameter and this one that needs to be added.
                result.append(template.substring(previousIndex, fullStart));
            }
            result.append(values.get(name));

            previousIndex = fullEnd;
        }
        if (previousIndex != template.length()) {
            result.append(template.substring(previousIndex));
        }

        return result;
    }
}
