package com.kwvanderlinde.discordant.mc;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;
import com.kwvanderlinde.discordant.core.modinterfaces.CommandEventHandler;
import com.kwvanderlinde.discordant.mc.integration.PlayerAdapter;
import com.kwvanderlinde.discordant.mc.integration.ProfileAdapter;
import com.kwvanderlinde.discordant.mc.integration.ServerAdapter;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;

import java.util.Collection;

public class DiscordantCommands {
    private static final SimpleCommandExceptionType MULTIPLE_PLAYERS = new SimpleCommandExceptionType(Component.literal("Only a single minecraft player is allowed"));

    private record CommandSourceStackResponder(CommandSourceStack source) implements CommandEventHandler.Responder {
        @Override
        public void success(SemanticMessage message) {
            source.sendSuccess(() -> message.reduce(ComponentRenderer.instance()), false);
        }

        @Override
        public void failure(SemanticMessage message) {
            source.sendFailure(message.reduce(ComponentRenderer.instance()));
        }
    }

    private boolean linkingEnabled = false;

    public void setLinkingCommandsEnabled(boolean linkingEnabled) {
        this.linkingEnabled = linkingEnabled;
    }

    public void register(CommandEventHandler commandEventHandler, CommandDispatcher<CommandSourceStack> dispatcher) {
        final var commands = Commands.literal("discord");

        commands
                // Toggle notification sounds.
                .then(Commands.literal("enable-mention-notification")
                              .executes(context -> sendNotifySoundState(context.getSource(), commandEventHandler))
                              .then(Commands.argument("soundstate", BoolArgumentType.bool())
                                            .executes(context -> switchNotifySoundState(context.getSource(), BoolArgumentType.getBool(context, "soundstate"), commandEventHandler))))
                // Linking commands only if linking is a possibility.
                .then(Commands.literal("link")
                              .requires(s -> linkingEnabled)
                              .executes(context -> link(context.getSource(), commandEventHandler))
                              .then(Commands.argument("playerName", GameProfileArgument.gameProfile())
                                            .suggests((commandContext, suggestionsBuilder) -> SharedSuggestionProvider.suggest(getLinkNamesSuggestions(commandContext), suggestionsBuilder))
                                            .requires(s -> s.hasPermission(4))
                                            .then(Commands.argument("discordId", LongArgumentType.longArg(0))
                                                          .executes(context -> adminLinkUser(context.getSource(),
                                                                                             commandEventHandler,
                                                                                             GameProfileArgument.getGameProfiles(context, "playerName"),
                                                                                             LongArgumentType.getLong(context, "discordId"))))))
                .then(Commands.literal("unlink")
                              .requires(s -> linkingEnabled)
                              .executes(context -> unlink(context.getSource(), commandEventHandler))
                              .then(Commands.argument("playerName", GameProfileArgument.gameProfile())
                                            .suggests((commandContext, suggestionsBuilder) -> SharedSuggestionProvider.suggest(getLinkNamesSuggestions(commandContext), suggestionsBuilder))
                                            .requires(s -> s.hasPermission(4))
                                            .executes(context -> adminUnlinkUser(context.getSource(),
                                                                                 commandEventHandler,
                                                                                 GameProfileArgument.getGameProfiles(context, "playerName"))))

                )
                .then(Commands.literal("list-linked")
                              .requires(s -> s.hasPermission(4))
                              .executes(context -> listLinkedProfiles(context.getSource(), commandEventHandler)))
                .then(Commands.literal("reload")
                              .requires(s -> s.hasPermission(4))
                              .executes(context -> { commandEventHandler.onReload(); return 0; }));

        dispatcher.register(commands);
    }

    private static String[] getLinkNamesSuggestions(CommandContext<CommandSourceStack> commandContext) {
        final var playerList = commandContext.getSource().getServer().getPlayerList();
        if (playerList.isUsingWhitelist()) {
            return playerList.getWhiteListNames();
        }
        return playerList.getPlayerNamesArray();
    }

    private static int link(CommandSourceStack source, CommandEventHandler handler) throws CommandSyntaxException {
        final var player = source.getPlayerOrException();
        handler.onLink(new PlayerAdapter(player), new CommandSourceStackResponder(source));
        return 0;
    }

    private static int adminLinkUser(CommandSourceStack source, CommandEventHandler handler, Collection<GameProfile> profiles, long discordId) throws CommandSyntaxException {
        final var profile = profiles.stream().findFirst();
        if (profile.isEmpty()) {
            throw MULTIPLE_PLAYERS.create();
        }
        handler.onAdminLinkUser(new ServerAdapter(source.getServer()),
                                new ProfileAdapter(profile.get()),
                                Long.toString(discordId, 10),
                                new CommandSourceStackResponder(source));
        return 0;
    }

    private static int adminUnlinkUser(CommandSourceStack source, CommandEventHandler handler, Collection<GameProfile> profiles) throws CommandSyntaxException {
        final var profile = profiles.stream().findFirst();
        if (profile.isEmpty()) {
            throw MULTIPLE_PLAYERS.create();
        }
        handler.onAdminUnlinkUser(new ServerAdapter(source.getServer()),
                                new ProfileAdapter(profile.get()),
                                new CommandSourceStackResponder(source));
        return 0;
    }

    private static int unlink(CommandSourceStack source, CommandEventHandler handler) throws CommandSyntaxException {
        final var player = source.getPlayerOrException();
        handler.onUnlink(new PlayerAdapter(player), new CommandSourceStackResponder(source));
        return 0;
    }

    private static int listLinkedProfiles(CommandSourceStack source, CommandEventHandler handler) throws CommandSyntaxException {
        final var player = source.getPlayerOrException();
        handler.onListLinkedProfiles(new PlayerAdapter(player), new CommandSourceStackResponder(source));
        return 0;
    }

    private static int sendNotifySoundState(CommandSourceStack source, CommandEventHandler handler) throws CommandSyntaxException {
        final var player = source.getPlayerOrException();
        handler.onQueryMentionNotificationsEnabled(new PlayerAdapter(player), new CommandSourceStackResponder(source));
        return 0;
    }

    private static int switchNotifySoundState(CommandSourceStack source, boolean state, CommandEventHandler handler) throws CommandSyntaxException {
        final var player = source.getPlayerOrException();
        handler.onSetMentionNotificationsEnabled(new PlayerAdapter(player), state, new CommandSourceStackResponder(source));
        return 0;
    }
}
