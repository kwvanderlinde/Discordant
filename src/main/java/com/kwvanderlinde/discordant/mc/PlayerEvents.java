package com.kwvanderlinde.discordant.mc;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.advancements.Advancement;
import net.minecraft.server.level.ServerPlayer;

/**
 * It is my firm desire that any custom events are made obsolete by fabric.
 */
public class PlayerEvents {
    /**
     * An event that is called when a player is awarded an advancement.
     * <p>
     * The closest fabric equivalent I can find is ServerMessageEvents.GAME_MESSAGE, but I can't
     * reliably tell what type of game message it is or extract the advancement information from it.
     */
    public static final Event<AdvancementAwarded> ADVANCEMENT_AWARDED = EventFactory.createArrayBacked(AdvancementAwarded.class, callbacks -> (player, advancement) -> {
        for (final var callback : callbacks) {
            callback.advancementAwarded(player, advancement);
        }
    });

    @FunctionalInterface
    public interface AdvancementAwarded {
        /**
         * Called when a player dies for realsies.
         *
         * @param player the player
         * @param advancement the awarded advancement
         */
        void advancementAwarded(ServerPlayer player, Advancement advancement);
    }
}
