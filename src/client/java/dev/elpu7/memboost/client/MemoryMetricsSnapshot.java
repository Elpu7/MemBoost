package dev.elpu7.memboost.client;

public record MemoryMetricsSnapshot(
        long usedBytes,
        long committedBytes,
        long maxBytes,
        long peakUsedBytes,
        long sampleCount,
        long lastSampleEpochMillis
) {

    private static final long MEBIBYTE = 1024L * 1024L;

    public long usedMiB() {
        return this.usedBytes / MEBIBYTE;
    }

    public long committedMiB() {
        return this.committedBytes / MEBIBYTE;
    }

    public long maxMiB() {
        return this.maxBytes / MEBIBYTE;
    }

    public long peakUsedMiB() {
        return this.peakUsedBytes / MEBIBYTE;
    }

    public int usagePercent() {
        if (this.maxBytes <= 0L) {
            return 0;
        }

        return (int) Math.min(100L, Math.round(this.usedBytes * 100.0 / this.maxBytes));
    }
}
