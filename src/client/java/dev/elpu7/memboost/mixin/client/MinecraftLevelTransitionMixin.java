package dev.elpu7.memboost.mixin.client;

import dev.elpu7.memboost.client.MemboostClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftLevelTransitionMixin {

    @Unique
    private ClientLevel memBoost$previousLevel;

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void memBoost$capturePreviousLevel(ClientLevel level, CallbackInfo ci) {
        this.memBoost$previousLevel = ((Minecraft) (Object) this).level;
    }

    @Inject(method = "setLevel", at = @At("RETURN"))
    private void memBoost$notifyLevelChange(ClientLevel level, CallbackInfo ci) {
        MemboostClient.handleWorldChanged(this.memBoost$previousLevel, level);
        this.memBoost$previousLevel = null;
    }
}
