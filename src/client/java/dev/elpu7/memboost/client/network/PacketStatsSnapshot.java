package dev.elpu7.memboost.client.network;

public record PacketStatsSnapshot(
        int recentChunkPackets,
        int recentLightPackets,
        int recentForgetPackets,
        int recentChunkBatchStarts,
        int recentChunkBatchFinishes,
        long totalChunkPackets,
        long totalLightPackets,
        long totalForgetPackets,
        long totalChunkBatchStarts,
        long totalChunkBatchFinishes,
        long burstActivationCount
) {

    public int recentTrafficScore() {
        return this.recentChunkPackets + this.recentLightPackets + this.recentForgetPackets;
    }
}
