package com.kwvanderlinde.discordant.core.modinterfaces;

import java.util.ArrayList;
import java.util.List;

public class BroadcastingCommandEventHandler implements CommandEventHandler {
    private final List<CommandEventHandler> handlers;

    public BroadcastingCommandEventHandler() {
        this.handlers = new ArrayList<>();
    }

    public void addHandler(CommandEventHandler handler) {
        this.handlers.add(handler);
    }

    @Override
    public void onAdminLinkUser(Server server, Profile profile, String discordId, Responder respondWith) {
        handlers.forEach(handler -> handler.onAdminLinkUser(server, profile, discordId, respondWith));
    }

    @Override
    public void onAdminUnlinkUser(Server server, Profile profile, Responder respondWith) {
        handlers.forEach(handler -> handler.onAdminUnlinkUser(server, profile, respondWith));
    }

    @Override
    public void onLink(Player player, Responder respondWith) {
        handlers.forEach(handler -> handler.onLink(player, respondWith));
    }

    @Override
    public void onUnlink(Player player, Responder respondWith) {
        handlers.forEach(handler -> handler.onUnlink(player, respondWith));
    }

    @Override
    public void onListLinkedProfiles(Player player, Responder respondWith) {
        handlers.forEach(handlers -> handlers.onListLinkedProfiles(player, respondWith));
    }

    @Override
    public void onQueryMentionNotificationsEnabled(Player player, Responder respondWith) {
        handlers.forEach(handler -> handler.onQueryMentionNotificationsEnabled(player, respondWith));
    }

    @Override
    public void onSetMentionNotificationsEnabled(Player player, boolean newState, Responder respondWith) {
        handlers.forEach(handler -> handler.onSetMentionNotificationsEnabled(player, newState, respondWith));
    }

    @Override
    public void onReload() {
        handlers.forEach(handler -> handler.onReload());
    }
}
