package com.kwvanderlinde.discordant.core.utils;

import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class StringUtils {
    public interface ChunkConsumer {
        void onMatch(MatchResult matchResult);

        void onBetween(String contents);
    }

    public static void chunk(Pattern pattern, String input, ChunkConsumer consumer) {
        int previousIndex = 0;
        final var matcher = pattern.matcher(input);
        while (matcher.find()) {
            final var fullStart = matcher.start(0);
            final var fullEnd = matcher.end(0);

            if (fullStart != previousIndex) {
                // There's stuff between the start of the string or previous match and the current
                // match.
                consumer.onBetween(input.substring(previousIndex, fullStart));
            }

            consumer.onMatch(matcher.toMatchResult());
            previousIndex = fullEnd;
        }
        if (previousIndex != input.length()) {
            consumer.onBetween(input.substring(previousIndex));
        }
    }
}
