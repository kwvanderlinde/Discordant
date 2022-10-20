package com.kwvanderlinde.discordant.core.modinterfaces;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

public interface PlayerEventHandler {
    @FunctionalInterface
    interface Rejector {
        void withReason(SemanticMessage reason);
    }

    void onPlayerSentMessage(Player player, String message, String plainTextCompositeMessage);
    void onPlayerJoinAttempt(Server server, Profile profile, Rejector reject);
    void onPlayerJoin(Player player);
    void onPlayerDisconnect(Player player);
    void onPlayerDeath(Player player, String message);
    void onPlayerAdvancement(Player player, Advancement advancement);
}
