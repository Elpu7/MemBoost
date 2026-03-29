package dev.elpu7.memboost.mixin.client;

import dev.elpu7.memboost.client.MemboostClient;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.multiplayer.ClientPacketListener.class)
abstract class ClientPacketListenerMixin {

    @Inject(method = "handleLogin", at = @At("RETURN"))
    private void memBoost$trackLoginRadius(ClientboundLoginPacket packet, CallbackInfo ci) {
        MemboostClient.handleServerChunkRadius(packet.chunkRadius());
    }

    @Inject(method = "handleSetChunkCacheRadius", at = @At("RETURN"))
    private void memBoost$trackUpdatedRadius(ClientboundSetChunkCacheRadiusPacket packet, CallbackInfo ci) {
        MemboostClient.handleServerChunkRadius(packet.getRadius());
    }
}
