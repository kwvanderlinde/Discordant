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
    public void onLink(Player player, Responder respondWith) {
        handlers.forEach(handler -> handler.onLink(player, respondWith));
    }

    @Override
    public void onUnlink(Player player, Responder respondWith) {
        handlers.forEach(handler -> handler.onUnlink(player, respondWith));
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
