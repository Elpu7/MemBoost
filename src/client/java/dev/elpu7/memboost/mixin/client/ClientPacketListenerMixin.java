package dev.elpu7.memboost.mixin.client;

import dev.elpu7.memboost.client.MemboostClient;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
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

    @Inject(method = "handleLevelChunkWithLight", at = @At("HEAD"))
    private void memBoost$trackChunkPacket(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
        MemboostClient.recordChunkPacket();
    }

    @Inject(method = "handleLightUpdatePacket", at = @At("HEAD"))
    private void memBoost$trackLightPacket(ClientboundLightUpdatePacket packet, CallbackInfo ci) {
        MemboostClient.recordLightPacket();
    }

    @Inject(method = "handleForgetLevelChunk", at = @At("HEAD"))
    private void memBoost$trackForgetPacket(ClientboundForgetLevelChunkPacket packet, CallbackInfo ci) {
        MemboostClient.recordForgetPacket();
    }

    @Inject(method = "handleChunkBatchStart", at = @At("HEAD"))
    private void memBoost$trackChunkBatchStart(ClientboundChunkBatchStartPacket packet, CallbackInfo ci) {
        MemboostClient.recordChunkBatchStart();
    }

    @Inject(method = "handleChunkBatchFinished", at = @At("HEAD"))
    private void memBoost$trackChunkBatchFinish(ClientboundChunkBatchFinishedPacket packet, CallbackInfo ci) {
        MemboostClient.recordChunkBatchFinish();
    }
}
