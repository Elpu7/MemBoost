package dev.elpu7.memboost.mixin.client;

import dev.elpu7.memboost.client.MemboostClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(net.minecraft.client.Minecraft.class)
abstract class MinecraftResourceReloadMixin {

    @Inject(method = "reloadResourcePacks", at = @At("HEAD"))
    private void memBoost$runCleanupBeforeResourceReload(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        MemboostClient.handleResourceReload();
    }
}
