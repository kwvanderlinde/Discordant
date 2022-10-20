package com.kwvanderlinde.discordant.mc.integration;

import com.kwvanderlinde.discordant.core.modinterfaces.BroadcastingCommandEventHandler;
import com.kwvanderlinde.discordant.core.modinterfaces.BroadcastingPlayerEventHandler;
import com.kwvanderlinde.discordant.core.modinterfaces.BroadcastingServerEventHandler;
import com.kwvanderlinde.discordant.core.modinterfaces.CommandEventHandler;
import com.kwvanderlinde.discordant.core.modinterfaces.PlayerEventHandler;
import com.kwvanderlinde.discordant.core.modinterfaces.Integration;
import com.kwvanderlinde.discordant.core.modinterfaces.ServerEventHandler;
import com.kwvanderlinde.discordant.mc.ComponentRenderer;
import com.kwvanderlinde.discordant.mc.DiscordantCommands;
import com.kwvanderlinde.discordant.mc.PlayerEvents;
import com.kwvanderlinde.discordant.mc.mixin.ServerLoginPacketListenerImpl_GameProfileAccessor;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dedicated.DedicatedServer;

import java.nio.file.Path;

public final class ModIntegration implements Integration {
    private final DiscordantCommands discordantCommands = new DiscordantCommands();
    private final BroadcastingCommandEventHandler commandEventHandler = new BroadcastingCommandEventHandler();
    private final BroadcastingServerEventHandler serverEventHandler = new BroadcastingServerEventHandler();
    private final BroadcastingPlayerEventHandler playerEventHandler = new BroadcastingPlayerEventHandler();

    public ModIntegration() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            discordantCommands.register(commandEventHandler, dispatcher);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverEventHandler.onServerStarted(new ServerAdapter(server));
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            serverEventHandler.onServerStopping(new ServerAdapter(server));
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            serverEventHandler.onServerStopped(new ServerAdapter(server));
        });
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            serverEventHandler.onTickStart(new ServerAdapter(server));
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            serverEventHandler.onTickEnd(new ServerAdapter(server));
        });


        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            final var adaptedPlayer = new PlayerAdapter(sender);
            final var plainMessage = message.signedContent().plain();
            final var plainTextCompositeMessage = Component.translatable("chat.type.text", sender.getDisplayName(), plainMessage).getString();
            playerEventHandler.onPlayerSentMessage(adaptedPlayer, plainMessage, plainTextCompositeMessage);
        });
        ServerLoginConnectionEvents.QUERY_START.register((netHandler, server, sender, synchronizer) -> {
            final var adaptedServer = new ServerAdapter(server);
            final var profile = new ProfileAdapter(((ServerLoginPacketListenerImpl_GameProfileAccessor) netHandler).getGameProfile());
            playerEventHandler.onPlayerJoinAttempt(adaptedServer, profile, reason -> netHandler.disconnect(reason.reduce(ComponentRenderer.instance())));
        });
        ServerPlayConnectionEvents.JOIN.register((netHandler, sender, server) -> {
            if (!netHandler.getConnection().isConnected()) {
                // Player connection must have been rejected.
                return;
            }
            final var adaptedPlayer = new PlayerAdapter(netHandler.getPlayer());
            playerEventHandler.onPlayerJoin(adaptedPlayer);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((netHandler, server) -> {
            final var adaptedPlayer = new PlayerAdapter(netHandler.getPlayer());
            playerEventHandler.onPlayerDisconnect(adaptedPlayer);
        });
        PlayerEvents.DEATH.register((player, damageSource) -> {
            final var adaptedPlayer = new PlayerAdapter(player);
            final var message = damageSource.getLocalizedDeathMessage(player).getString();
            playerEventHandler.onPlayerDeath(adaptedPlayer, message);
        });
        PlayerEvents.ADVANCEMENT_AWARDED.register((player, advancement) -> {
            final var adaptedPlayer = new PlayerAdapter(player);
            final var adaptedAdvancement = new AdvancementAdapter(
                    advancement.getDisplay().getFrame().getName(),
                    advancement.getDisplay().getTitle().getString(),
                    advancement.getDisplay().getDescription().getString()
            );
            playerEventHandler.onPlayerAdvancement(adaptedPlayer, adaptedAdvancement);
        });
    }

    @Override
    public Path getConfigRoot() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void setLinkingCommandsEnabled(boolean linkingEnabled) {
        discordantCommands.setLinkingCommandsEnabled(linkingEnabled);
    }

    @Override
    public void addHandler(ServerEventHandler handler) {
        serverEventHandler.addHandler(handler);
    }

    @Override
    public void addHandler(PlayerEventHandler handler) {
        playerEventHandler.addHandler(handler);
    }

    @Override
    public void addHandler(CommandEventHandler handler) {
        commandEventHandler.addHandler(handler);
    }
}
