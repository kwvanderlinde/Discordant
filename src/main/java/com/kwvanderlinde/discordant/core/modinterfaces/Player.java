package com.kwvanderlinde.discordant.core.modinterfaces;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import java.util.UUID;

public interface Player {
    Profile profile();

    UUID uuid();

    String name();

    void sendSystemMessage(SemanticMessage message);

    boolean isMentionNotificationsEnabled();

    void setMentionNotificationsEnabled(boolean enabled);

    void notifySound();
}
