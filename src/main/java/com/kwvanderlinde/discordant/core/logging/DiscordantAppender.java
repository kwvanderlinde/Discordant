package com.kwvanderlinde.discordant.core.logging;

import com.kwvanderlinde.discordant.core.discord.DiscordApi;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.filter.LevelMatchFilter;
import org.apache.logging.log4j.core.layout.MessageLayout;

import java.text.SimpleDateFormat;

public class DiscordantAppender extends AbstractAppender {
    private final SimpleDateFormat timestampFormatter = new SimpleDateFormat("hh:mm:ss");
    private final DiscordApi discordApi;

    public DiscordantAppender(Level level, DiscordApi discordApi) {
        super("Discordant",
              new LevelMatchFilter.Builder().setLevel(level).build(),
              new MessageLayout(),
              false,
              Property.EMPTY_ARRAY);

        this.discordApi = discordApi;

        this.start();
    }

    @Override
    public void append(LogEvent event) {
        String sb = String.format("[%s] [%s/%s]: %s",
                                  timestampFormatter.format(event.getTimeMillis()),
                                  event.getThreadName(),
                                  event.getLevel(),
                                  event.getMessage().getFormattedMessage());
        discordApi.postConsoleMessage(sb);
    }
}
