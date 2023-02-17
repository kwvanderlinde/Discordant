package com.kwvanderlinde.discordant.mc.integration;

import com.kwvanderlinde.discordant.core.modinterfaces.Player;
import com.kwvanderlinde.discordant.core.modinterfaces.Server;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;

import java.util.UUID;
import java.util.stream.Stream;

public final class ServerAdapter implements Server {
    private final MinecraftServer server;

    public ServerAdapter(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public int getTickCount() {
        return server.getTickCount();
    }

    @Override
    public int getPlayerCount() {
        return server.getPlayerCount();
    }

    @Override
    public int getMaxPlayers() {
        return server.getMaxPlayers();
    }

    @Override
    public Player getPlayer(UUID uuid) {
        final var internalPlayer = server.getPlayerList().getPlayer(uuid);
        if (internalPlayer == null) {
            return null;
        }
        return new PlayerAdapter(internalPlayer);
    }

    @Override
    public Stream<Player> getAllPlayers() {
        return server.getPlayerList().getPlayers().stream().map(PlayerAdapter::new);
    }

    @Override
    public String motd() {
        return server.getMotd();
    }

    @Override
    public void runCommand(String command) {
        if (server instanceof DedicatedServer dedicatedServer) {
            server.execute(() -> dedicatedServer.runCommand(command));
        }
        // TODO Should we account for the non-dedicated server cast? What would it mean?
    }
}
