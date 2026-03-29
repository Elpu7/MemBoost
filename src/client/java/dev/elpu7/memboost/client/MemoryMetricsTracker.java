package dev.elpu7.memboost.client;

import dev.elpu7.memboost.config.MemBoostConfig;

public final class MemoryMetricsTracker {

    private final Runtime runtime = Runtime.getRuntime();
    private long tickCounter;
    private long currentUsedBytes;
    private long currentCommittedBytes;
    private long maxBytes;
    private long peakUsedBytes;
    private long sampleCount;
    private long lastSampleEpochMillis;

    public void tick(MemBoostConfig config) {
        this.tickCounter++;

        if (this.tickCounter % Math.max(1, config.getSampleIntervalTicks()) == 0L) {
            sampleNow();
        }
    }

    public void sampleNow() {
        long committedBytes = this.runtime.totalMemory();
        long usedBytes = committedBytes - this.runtime.freeMemory();

        this.currentCommittedBytes = committedBytes;
        this.currentUsedBytes = usedBytes;
        this.maxBytes = this.runtime.maxMemory();
        this.peakUsedBytes = Math.max(this.peakUsedBytes, usedBytes);
        this.sampleCount++;
        this.lastSampleEpochMillis = System.currentTimeMillis();
    }

    public void resetPeak() {
        this.peakUsedBytes = this.currentUsedBytes;
    }

    public MemoryMetricsSnapshot snapshot() {
        return new MemoryMetricsSnapshot(
                this.currentUsedBytes,
                this.currentCommittedBytes,
                this.maxBytes,
                this.peakUsedBytes,
                this.sampleCount,
                this.lastSampleEpochMillis
        );
    }
}
