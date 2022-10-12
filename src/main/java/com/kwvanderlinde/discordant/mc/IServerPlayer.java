package com.kwvanderlinde.discordant.mc;

public interface IServerPlayer {
    boolean getNotifyState();

    void setNotifyState(boolean bl);

    boolean isAcceptingChatMessages();
}
