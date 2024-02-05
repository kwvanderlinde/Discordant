package com.kwvanderlinde.discordant.core.modinterfaces;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

public interface CommandEventHandler {
    void onAdminLinkUser(Server server, Profile profile, String discordId, Responder respondWith);
    void onAdminUnlinkUser(Server server, Profile profile, Responder respondWith);
    void onLink(Player player, Responder respondWith);
    void onUnlink(Player player, Responder respondWith);
    void onListLinkedProfiles(Player player, Responder respondWith);
    void onQueryMentionNotificationsEnabled(Player player, Responder respondWith);
    void onSetMentionNotificationsEnabled(Player player, boolean newState, Responder respondWith);
    void onReload();


    interface Responder {
        void success(SemanticMessage message);

        void failure(SemanticMessage message);
    }
}
