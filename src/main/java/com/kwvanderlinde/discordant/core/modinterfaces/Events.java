package com.kwvanderlinde.discordant.core.modinterfaces;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

// TODO Don't name this interface after a design pattern.
public interface Events {
    void onServerStarted(ServerStartedHandler callback);
    void onServerStopping(ServerStoppingHandler callback);
    void onServerStopped(ServerStoppedHandler callback);
    void onTickStart(TickStartHandler callback);
    void onTickEnd(TickEndHandler callback);

    void onPlayerSentMessage(PlayerMessageSendHandler handler);
    void onPlayerJoinAttempt(PlayerJoinAttemptHandler handler);
    void onPlayerJoin(PlayerJoinHandler handler);
    void onPlayerDisconnect(PlayerDisconnectHandler handler);
    void onPlayerDeath(PlayerDeathHandler handler);
    void onPlayerAdvancement(PlayerAdvancementHandler handler);

    @FunctionalInterface
    interface Rejector {
        void withReason(SemanticMessage reason);
    }

    @FunctionalInterface
    interface ServerStartedHandler {
        void started(Server server);
    }

    @FunctionalInterface
    interface ServerStoppingHandler {
        void stopping(Server server);
    }

    @FunctionalInterface
    interface ServerStoppedHandler {
        void stopped(Server server);
    }

    @FunctionalInterface
    interface TickStartHandler {
        void tickStart(Server server);
    }

    @FunctionalInterface
    interface TickEndHandler {
        void tickEnd(Server server);
    }

    @FunctionalInterface
    interface PlayerMessageSendHandler {
        void messageSent(Player player, String message, String plainTextCompositeMessage);
    }

    @FunctionalInterface
    interface PlayerJoinAttemptHandler {
        void joinAttempted(Profile profile, Rejector reject);
    }

    @FunctionalInterface
    interface PlayerJoinHandler {
        void joined(Player player);
    }

    @FunctionalInterface
    interface PlayerDisconnectHandler {
        void disconnected(Player player);
    }

    @FunctionalInterface
    interface PlayerDeathHandler {
        void died(Player player, String message);
    }

    @FunctionalInterface
    interface PlayerAdvancementHandler {
        void advancementAwarded(Player player, Advancement advancement);
    }
}
