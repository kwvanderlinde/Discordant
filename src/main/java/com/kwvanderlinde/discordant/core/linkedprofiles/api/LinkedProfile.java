package com.kwvanderlinde.discordant.core.linkedprofiles.api;

import java.util.UUID;

public class LinkedProfile {
    private final String name;
    private final UUID uuid;
    private final String discordId;

    public LinkedProfile(String name, UUID uuid, String discordId) {
        this.name = name;
        this.uuid = uuid;
        this.discordId = discordId;
    }

    public String name() {
        return name;
    }

    public UUID uuid() {
        return uuid;
    }

    public String discordId() {
        return discordId;
    }
}
