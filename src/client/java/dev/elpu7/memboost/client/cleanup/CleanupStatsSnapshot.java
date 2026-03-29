package dev.elpu7.memboost.client.cleanup;

public record CleanupStatsSnapshot(
        long pressureCleanupCount,
        long worldChangeCleanupCount,
        long disconnectCleanupCount,
        long mapTextureResetCount,
        long particleClearCount,
        long lastCleanupEpochMillis,
        String lastCleanupReason,
        int loadedChunks
) {

    public long totalCleanupCount() {
        return this.pressureCleanupCount + this.worldChangeCleanupCount + this.disconnectCleanupCount;
    }

    public boolean hasCleanupRun() {
        return this.lastCleanupEpochMillis > 0L;
    }

    public String describeLastCleanup() {
        return hasCleanupRun() ? this.lastCleanupReason : "none";
    }
}
