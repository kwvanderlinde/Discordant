package com.kwvanderlinde.discordant.core.modinterfaces;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

public interface CommandEventHandler {
    void onLink(Player player, Responder respondWith);
    void onUnlink(Player player, Responder respondWith);
    void onQueryMentionNotificationsEnabled(Player player, Responder respondWith);
    void onSetMentionNotificationsEnabled(Player player, boolean newState, Responder respondWith);
    void onReload();


    interface Responder {
        void success(SemanticMessage message);

        void failure(SemanticMessage message);
    }
}
