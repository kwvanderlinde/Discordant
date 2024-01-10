package com.kwvanderlinde.discordant.mc.mixin;

import com.kwvanderlinde.discordant.mc.PlayerEvents;
import net.minecraft.advancements.AdvancementHolder;
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

    @Inject(method = "award", at = @At(value = "INVOKE", target = "net/minecraft/advancements/AdvancementRewards.grant (Lnet/minecraft/server/level/ServerPlayer;)V", shift = At.Shift.AFTER))
    private void sendAdvancement(AdvancementHolder advancement, String string, CallbackInfoReturnable<Boolean> cir) {
        // Advancement reward was granted.
        PlayerEvents.ADVANCEMENT_AWARDED.invoker().advancementAwarded(this.player, advancement.value());
    }
}
