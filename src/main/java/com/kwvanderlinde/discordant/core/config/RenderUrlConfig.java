package com.kwvanderlinde.discordant.core.config;

import java.util.HashMap;
import java.util.Map;

public class RenderUrlConfig {
    public String icon = "https://crafatar.com/avatars/{player.uuid}/?size=16&overlay&ts={server.time}";
    public Map<String, String> other = new HashMap<>();

    {
        other.put("head", "https://crafatar.com/renders/head/{player.uuid}/?scale=10&overlay&ts={server.time}");
        other.put("body", "https://crafatar.com/renders/body/{player.uuid}/?scale=10&overlay&ts={server.time}");
    }
}
