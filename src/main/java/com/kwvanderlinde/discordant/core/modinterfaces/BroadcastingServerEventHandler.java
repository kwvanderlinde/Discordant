package com.kwvanderlinde.discordant.core.modinterfaces;

import java.util.ArrayList;
import java.util.List;

public class BroadcastingServerEventHandler implements ServerEventHandler {
    private final List<ServerEventHandler> handlers;

    public BroadcastingServerEventHandler() {
        this.handlers = new ArrayList<>();
    }

    public void addHandler(ServerEventHandler handler) {
        this.handlers.add(handler);
    }

    @Override
    public void onServerStarted(Server server) {
        handlers.forEach(handler -> handler.onServerStarted(server));
    }

    @Override
    public void onServerStopping(Server server) {
        handlers.forEach(handler -> handler.onServerStopping(server));
    }

    @Override
    public void onServerStopped(Server server) {
        handlers.forEach(handler -> handler.onServerStopped(server));
    }

    @Override
    public void onTickStart(Server server) {
        handlers.forEach(handler -> handler.onTickStart(server));
    }

    @Override
    public void onTickEnd(Server server) {
        handlers.forEach(handler -> handler.onTickEnd(server));
    }
}
