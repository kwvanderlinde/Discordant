package com.kwvanderlinde.discordant.core.messages;

import com.kwvanderlinde.discordant.core.utils.StringUtils;

import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class ScopeUtil {
    private static final Pattern parameterPattern = Pattern.compile("\\{([a-zA-Z.|]*)}");

    public static SemanticMessage instantiate(String template, Map<String, SemanticMessage.Part> values) {
        final var result = new SemanticMessage();

        StringUtils.chunk(parameterPattern, template, new StringUtils.ChunkConsumer() {
            @Override
            public void onMatch(MatchResult matchResult) {
                final var name = matchResult.group(1);
                if (values.containsKey(name)) {
                    result.append(values.get(name));
                }
                else {
                    // Unknown parameter, leave the whole thing in there, curly braces and all.
                    result.append(matchResult.group(0));
                }
            }

            @Override
            public void onBetween(String contents) {
                result.append(contents);
            }
        });

        return result;
    }
}
