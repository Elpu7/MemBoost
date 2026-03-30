package dev.elpu7.memboost.client.cleanup;

public record CleanupStatsSnapshot(
        long pressureCleanupCount,
        long worldChangeCleanupCount,
        long disconnectCleanupCount,
        long resourceReloadCleanupCount,
        long mapTextureResetCount,
        long particleClearCount,
        long transientStateResetCount,
        long lastCleanupEpochMillis,
        String lastCleanupReason,
        int loadedChunks,
        int serverChunkRadius,
        int activeChunkRadius,
        long chunkPressureActivationCount,
        long packetBurstCleanupCount
) {

    public long totalCleanupCount() {
        return this.pressureCleanupCount + this.worldChangeCleanupCount + this.disconnectCleanupCount + this.resourceReloadCleanupCount;
    }

    public boolean hasCleanupRun() {
        return this.lastCleanupEpochMillis > 0L;
    }

    public String describeLastCleanup() {
        return hasCleanupRun() ? this.lastCleanupReason : "none";
    }
}
