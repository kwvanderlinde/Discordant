package com.kwvanderlinde.discordant.mc;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/**
 * It is my firm desire that any custom events are made obsolete by fabric.
 */
public class PlayerEvents {
    /**
     * An event that is called when a player dies.
     * <p>
     * Hopefully we can get rid of this soon if PR#2573 goes through.
     *
     * @see <a href="https://github.com/FabricMC/fabric/pull/2573">#2573</a>
     */
    public static final Event<Death> DEATH = EventFactory.createArrayBacked(Death.class, callbacks -> (player, damageSource) -> {
        for (final var callback : callbacks) {
            callback.afterDeath(player, damageSource);
        }
    });

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
    public interface Death {
        /**
         * Called when a player dies for realsies.
         *
         * @param player the player
         * @param damageSource the fatal damage damageSource
         */
        void afterDeath(ServerPlayer player, DamageSource damageSource);
    }

    @FunctionalInterface
    public interface AdvancementAwarded {
        /**
         * Called when a player dies for realsies.
         *
         * @param player the player
         * @param advancement the awarded advancment
         */
        void advancementAwarded(ServerPlayer player, Advancement advancement);
    }

    @FunctionalInterface
    public interface ChatMessageSent {
        /**
         * Called when a player sends a chat message.
         *
         * @param player the player
         * @param message the message
         * @param textComponent the message as a component?
         */
        void onMessage(ServerPlayer player, String message, MutableComponent textComponent);
    }
}
