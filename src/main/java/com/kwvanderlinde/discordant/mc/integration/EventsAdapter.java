package com.kwvanderlinde.discordant.mc.integration;

import com.kwvanderlinde.discordant.core.modinterfaces.Events;
import com.kwvanderlinde.discordant.mc.ComponentRenderer;
import com.kwvanderlinde.discordant.mc.PlayerEvents;
import com.kwvanderlinde.discordant.mc.integration.AdvancementAdapter;
import com.kwvanderlinde.discordant.mc.integration.PlayerAdapter;
import com.kwvanderlinde.discordant.mc.integration.ProfileAdapter;
import com.kwvanderlinde.discordant.mc.integration.ServerAdapter;
import com.kwvanderlinde.discordant.mc.mixin.ServerLoginPacketListenerImpl_GameProfileAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dedicated.DedicatedServer;

public final class EventsAdapter implements Events {
    @Override
    public void onServerStarted(ServerStartedHandler handler) {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            handler.started(new ServerAdapter((DedicatedServer) server));
        });
    }

    @Override
    public void onServerStopping(ServerStoppingHandler handler) {
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            handler.stopping(new ServerAdapter((DedicatedServer) server));
        });
    }

    @Override
    public void onServerStopped(ServerStoppedHandler handler) {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            handler.stopped(new ServerAdapter((DedicatedServer) server));
        });
    }

    @Override
    public void onTickStart(TickStartHandler handler) {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            handler.tickStart(new ServerAdapter((DedicatedServer) server));
        });
    }

    @Override
    public void onTickEnd(TickEndHandler handler) {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            handler.tickEnd(new ServerAdapter((DedicatedServer) server));
        });
    }

    @Override
    public void onPlayerSentMessage(PlayerMessageSendHandler handler) {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            final var plainMessage = message.signedContent().plain();
            handler.messageSent(new PlayerAdapter(sender),
                                plainMessage,
                                Component.translatable("chat.type.text", sender.getDisplayName(), plainMessage).getString());
        });
    }

    @Override
    public void onPlayerJoinAttempt(PlayerJoinAttemptHandler handler) {
        ServerLoginConnectionEvents.QUERY_START.register((netHandler, server, sender, synchronizer) -> {
            final var profile = ((ServerLoginPacketListenerImpl_GameProfileAccessor) netHandler).getGameProfile();
            handler.joinAttempted(
                    new ServerAdapter((DedicatedServer) server),
                    new ProfileAdapter(profile),
                    reason -> netHandler.disconnect(reason.reduce(ComponentRenderer.instance()))
            );
        });
    }

    @Override
    public void onPlayerJoin(PlayerJoinHandler handler) {
        ServerPlayConnectionEvents.JOIN.register((netHandler, sender, server) -> {
            if (!netHandler.getConnection().isConnected()) {
                // Player connection must have been rejected.
                return;
            }

            handler.joined(
                    new PlayerAdapter(netHandler.getPlayer())
            );
        });
    }

    @Override
    public void onPlayerDisconnect(PlayerDisconnectHandler handler) {
        ServerPlayConnectionEvents.DISCONNECT.register((netHandler, server) -> {
            handler.disconnected(
                    new PlayerAdapter(netHandler.getPlayer())
            );
        });
    }

    @Override
    public void onPlayerDeath(PlayerDeathHandler handler) {
        PlayerEvents.DEATH.register((player, damageSource) -> {
            handler.died(
                    new PlayerAdapter(player),
                    damageSource.getLocalizedDeathMessage(player).getString()
            );
        });
    }

    @Override
    public void onPlayerAdvancement(PlayerAdvancementHandler handler) {
        PlayerEvents.ADVANCEMENT_AWARDED.register((player, advancement) -> {
            handler.advancementAwarded(
                    new PlayerAdapter(player),
                    new AdvancementAdapter(
                            advancement.getDisplay().getFrame().getName(),
                            advancement.getDisplay().getTitle().getString(),
                            advancement.getDisplay().getDescription().getString()
                    )
            );
        });
    }
}
