package com.kwvanderlinde.discordant.core.discord;

public class LinkedProfile {
    private final String name;
    private final String uuid;
    private final String discordId;

    public LinkedProfile(String name, String uuid, String discordId) {
        this.name = name;
        this.uuid = uuid;
        this.discordId = discordId;
    }

    public String name() {
        return name;
    }

    public String uuid() {
        return uuid;
    }

    public String discordId() {
        return discordId;
    }
}
