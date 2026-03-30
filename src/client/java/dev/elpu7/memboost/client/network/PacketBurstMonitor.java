package dev.elpu7.memboost.client.network;

public final class PacketBurstMonitor {

    private static final int WINDOW_TICKS = 20;

    private final int[] chunkPackets = new int[WINDOW_TICKS];
    private final int[] lightPackets = new int[WINDOW_TICKS];
    private final int[] forgetPackets = new int[WINDOW_TICKS];
    private final int[] chunkBatchStarts = new int[WINDOW_TICKS];
    private final int[] chunkBatchFinishes = new int[WINDOW_TICKS];

    private int tickIndex;
    private int recentChunkPackets;
    private int recentLightPackets;
    private int recentForgetPackets;
    private int recentChunkBatchStarts;
    private int recentChunkBatchFinishes;
    private long totalChunkPackets;
    private long totalLightPackets;
    private long totalForgetPackets;
    private long totalChunkBatchStarts;
    private long totalChunkBatchFinishes;
    private long burstActivationCount;

    public void tick() {
        this.tickIndex = (this.tickIndex + 1) % WINDOW_TICKS;

        this.recentChunkPackets -= this.chunkPackets[this.tickIndex];
        this.recentLightPackets -= this.lightPackets[this.tickIndex];
        this.recentForgetPackets -= this.forgetPackets[this.tickIndex];
        this.recentChunkBatchStarts -= this.chunkBatchStarts[this.tickIndex];
        this.recentChunkBatchFinishes -= this.chunkBatchFinishes[this.tickIndex];

        this.chunkPackets[this.tickIndex] = 0;
        this.lightPackets[this.tickIndex] = 0;
        this.forgetPackets[this.tickIndex] = 0;
        this.chunkBatchStarts[this.tickIndex] = 0;
        this.chunkBatchFinishes[this.tickIndex] = 0;
    }

    public void recordChunkPacket() {
        this.chunkPackets[this.tickIndex]++;
        this.recentChunkPackets++;
        this.totalChunkPackets++;
    }

    public void recordLightPacket() {
        this.lightPackets[this.tickIndex]++;
        this.recentLightPackets++;
        this.totalLightPackets++;
    }

    public void recordForgetPacket() {
        this.forgetPackets[this.tickIndex]++;
        this.recentForgetPackets++;
        this.totalForgetPackets++;
    }

    public void recordChunkBatchStart() {
        this.chunkBatchStarts[this.tickIndex]++;
        this.recentChunkBatchStarts++;
        this.totalChunkBatchStarts++;
    }

    public void recordChunkBatchFinish() {
        this.chunkBatchFinishes[this.tickIndex]++;
        this.recentChunkBatchFinishes++;
        this.totalChunkBatchFinishes++;
    }

    public void markBurstActivation() {
        this.burstActivationCount++;
    }

    public void resetWindow() {
        this.tickIndex = 0;
        this.recentChunkPackets = 0;
        this.recentLightPackets = 0;
        this.recentForgetPackets = 0;
        this.recentChunkBatchStarts = 0;
        this.recentChunkBatchFinishes = 0;

        for (int i = 0; i < WINDOW_TICKS; i++) {
            this.chunkPackets[i] = 0;
            this.lightPackets[i] = 0;
            this.forgetPackets[i] = 0;
            this.chunkBatchStarts[i] = 0;
            this.chunkBatchFinishes[i] = 0;
        }
    }

    public PacketStatsSnapshot snapshot() {
        return new PacketStatsSnapshot(
                this.recentChunkPackets,
                this.recentLightPackets,
                this.recentForgetPackets,
                this.recentChunkBatchStarts,
                this.recentChunkBatchFinishes,
                this.totalChunkPackets,
                this.totalLightPackets,
                this.totalForgetPackets,
                this.totalChunkBatchStarts,
                this.totalChunkBatchFinishes,
                this.burstActivationCount
        );
    }
}
