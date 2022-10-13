package com.kwvanderlinde.discordant.mc.integration;

import com.kwvanderlinde.discordant.core.modinterfaces.CommandHandlers;
import com.kwvanderlinde.discordant.core.modinterfaces.Events;
import com.kwvanderlinde.discordant.core.modinterfaces.Integration;
import com.kwvanderlinde.discordant.mc.DiscordantCommands;
import com.kwvanderlinde.discordant.mc.integration.EventsAdapter;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class ModIntegration implements Integration {
    private final Events events = new EventsAdapter();
    private final CommandHandlers commandHandlers = new CommandHandlers();

    @Override
    public Path getConfigRoot() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void enableCommands(boolean linkingEnabled) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            DiscordantCommands.register(commandHandlers, dispatcher, linkingEnabled);
        });
    }

    @Override
    public Events events() {
        return events;
    }

    @Override
    public CommandHandlers commandsHandlers() {
        return commandHandlers;
    }
}
