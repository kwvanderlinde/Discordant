package com.kwvanderlinde.discordant.mixin;

import com.kwvanderlinde.discordant.discord.Discordant;
import net.minecraft.advancements.Advancement;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixins {
    @Shadow
    private ServerPlayer player;

    @Inject(method = "award", at = @At(value = "INVOKE", target = "net/minecraft/server/players/PlayerList.broadcastSystemMessage (Lnet/minecraft/network/chat/Component;Lnet/minecraft/resources/ResourceKey;)V", shift = At.Shift.AFTER))
    private void sendAdvancement(Advancement advancement, String string, CallbackInfoReturnable<Boolean> cir) {
        Discordant.sendAdvancement(this.player.getScoreboardName(), advancement, this.player.getStringUUID());
    }
}
