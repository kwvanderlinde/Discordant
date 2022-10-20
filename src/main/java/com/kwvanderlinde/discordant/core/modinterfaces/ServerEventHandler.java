package com.kwvanderlinde.discordant.core.modinterfaces;

public interface ServerEventHandler {
    void onServerStarted(Server server);
    void onServerStopping(Server server);
    void onServerStopped(Server server);
    void onTickStart(Server server);
    void onTickEnd(Server server);
}
