package ru.aiefu.discordlink.config;

public class LinkedProfile {
    public final String name;
    public final String uuid;
    public final String discordId;

    public LinkedProfile(String name, String uuid, String discordId){
        this.name = name;
        this.uuid = uuid;
        this.discordId = discordId;
    }
}
