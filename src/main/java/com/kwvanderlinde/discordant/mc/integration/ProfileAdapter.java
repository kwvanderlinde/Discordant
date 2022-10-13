package com.kwvanderlinde.discordant.mc.integration;

import com.kwvanderlinde.discordant.core.modinterfaces.Profile;
import com.mojang.authlib.GameProfile;

import java.util.UUID;

public final class ProfileAdapter implements Profile {
    private final GameProfile profile;

    public ProfileAdapter(GameProfile profile) {
        this.profile = profile;
    }

    @Override
    public UUID uuid() {
        return profile.getId();
    }

    @Override
    public String name() {
        return profile.getName();
    }
}
