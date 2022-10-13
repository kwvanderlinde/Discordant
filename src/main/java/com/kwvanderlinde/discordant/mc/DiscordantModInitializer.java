package com.kwvanderlinde.discordant.mc;

import com.kwvanderlinde.discordant.core.Discordant;
import com.kwvanderlinde.discordant.mc.integration.ModIntegration;
import net.fabricmc.api.DedicatedServerModInitializer;

public class DiscordantModInitializer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        Discordant.initialize(new ModIntegration());
    }
}
