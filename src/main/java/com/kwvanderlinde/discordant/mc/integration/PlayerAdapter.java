package com.kwvanderlinde.discordant.mc.integration;

import com.kwvanderlinde.discordant.core.messages.SemanticMessage;
import com.kwvanderlinde.discordant.core.modinterfaces.Player;
import com.kwvanderlinde.discordant.core.modinterfaces.Profile;
import com.kwvanderlinde.discordant.core.modinterfaces.Server;
import com.kwvanderlinde.discordant.mc.ComponentRenderer;
import com.kwvanderlinde.discordant.mc.IServerPlayer;
import com.kwvanderlinde.discordant.mc.integration.ProfileAdapter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.UUID;

public class PlayerAdapter implements Player {
    private final ServerPlayer player;

    public PlayerAdapter(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public Profile profile() {
        return new ProfileAdapter(player.getGameProfile());
    }

    @Override
    public Server server() {
        return new ServerAdapter(player.getServer());
    }

    @Override
    public UUID uuid() {
        return player.getUUID();
    }

    @Override
    public String name() {
        return player.getScoreboardName();
    }

    @Override
    public void sendSystemMessage(SemanticMessage message) {
        // TODO Why aren't we checking if system messages are accepting.
        if (((IServerPlayer) player).isAcceptingChatMessages()) {
            player.sendSystemMessage(message.reduce(new ComponentRenderer()));
        }
    }

    @Override
    public boolean isMentionNotificationsEnabled() {
        return ((IServerPlayer) player).getNotifyState();
    }

    @Override
    public void setMentionNotificationsEnabled(boolean enabled) {
        ((IServerPlayer) player).setNotifyState(enabled);
    }

    @Override
    public void notifySound() {
        final var serverPlayer = (IServerPlayer) player;
        if (serverPlayer.getNotifyState()) {
            player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING, SoundSource.MASTER, 1.0F, 1.0F);
        }
    }
}
