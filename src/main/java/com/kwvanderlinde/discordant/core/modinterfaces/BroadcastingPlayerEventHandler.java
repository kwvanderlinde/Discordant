package com.kwvanderlinde.discordant.core.modinterfaces;

import java.util.ArrayList;
import java.util.List;

public class BroadcastingPlayerEventHandler implements PlayerEventHandler {
    private final List<PlayerEventHandler> handlers;

    public BroadcastingPlayerEventHandler() {
        this.handlers = new ArrayList<>();
    }

    public void addHandler(PlayerEventHandler handler) {
        this.handlers.add(handler);
    }

    @Override
    public void onPlayerSentMessage(Player player, String message, String plainTextCompositeMessage) {
        handlers.forEach(handler -> handler.onPlayerSentMessage(player, message, plainTextCompositeMessage));
    }

    @Override
    public void onPlayerJoinAttempt(Server server, Profile profile, Rejector reject) {
        handlers.forEach(handler -> handler.onPlayerJoinAttempt(server, profile, reject));
    }

    @Override
    public void onPlayerJoin(Player player) {
        handlers.forEach(handler -> handler.onPlayerJoin(player));
    }

    @Override
    public void onPlayerDisconnect(Player player) {
        handlers.forEach(handler -> handler.onPlayerDisconnect(player));
    }

    @Override
    public void onPlayerDeath(Player player, String message) {
        handlers.forEach(handler -> handler.onPlayerDeath(player, message));
    }

    @Override
    public void onPlayerAdvancement(Player player, Advancement advancement) {
        handlers.forEach(handler -> handler.onPlayerAdvancement(player, advancement));
    }
}
