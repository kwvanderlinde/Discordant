package com.kwvanderlinde.discordant.core.modinterfaces;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

public class CommandHandlers {
    public LinkHandler link = (player, respondWith) -> {};
    public UnlinkHandler unlink = (player, respondWith) -> {};
    public QueryMentionNotificationEnabledsHandler queryMentionNotificationsEnabled = (player, respondWith) -> {};
    public SetMentionNotificationsHandler setMentionNotificationsEnabled = (player, newState, respondWith) -> {};

    @FunctionalInterface
    public interface LinkHandler {
        void handle(Player player, Responder respondWith);
    }

    @FunctionalInterface
    public interface UnlinkHandler {
        void handle(Player player, Responder respondWith);
    }

    @FunctionalInterface
    public interface QueryMentionNotificationEnabledsHandler {
        void handle(Player player, Responder respondWith);
    }

    @FunctionalInterface
    public interface SetMentionNotificationsHandler {
        void handle(Player player, boolean newState, Responder respondWith);
    }

    public interface Responder {
        void success(SemanticMessage message);

        void failure(SemanticMessage message);
    }
}
