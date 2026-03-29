package dev.elpu7.memboost.client.cleanup;

import dev.elpu7.memboost.Memboost;
import dev.elpu7.memboost.client.MemoryMetricsSnapshot;
import dev.elpu7.memboost.client.MemoryMetricsTracker;
import dev.elpu7.memboost.config.MemBoostConfig;
import dev.elpu7.memboost.config.OptimizationProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class MemBoostCleanupCoordinator {

    private static final long BALANCED_PRESSURE_COOLDOWN_MS = 15_000L;
    private static final long AGGRESSIVE_PRESSURE_COOLDOWN_MS = 7_500L;

    private long pressureCleanupCount;
    private long worldChangeCleanupCount;
    private long disconnectCleanupCount;
    private long mapTextureResetCount;
    private long particleClearCount;
    private long lastCleanupEpochMillis;
    private long lastPressureCleanupEpochMillis;
    private String lastCleanupReason = "none";

    public void tick(Minecraft client, MemBoostConfig config, MemoryMetricsTracker metricsTracker) {
        OptimizationProfile profile = config.getProfile();

        if (profile == OptimizationProfile.SAFE) {
            return;
        }

        MemoryMetricsSnapshot snapshot = metricsTracker.snapshot();

        if (snapshot.usagePercent() < config.getWarningThresholdPercent()) {
            return;
        }

        long now = System.currentTimeMillis();
        long cooldownMs = profile == OptimizationProfile.AGGRESSIVE
                ? AGGRESSIVE_PRESSURE_COOLDOWN_MS
                : BALANCED_PRESSURE_COOLDOWN_MS;

        if (now - this.lastPressureCleanupEpochMillis < cooldownMs) {
            return;
        }

        boolean mapsReset = resetMapTextures(client);
        boolean particlesCleared = false;

        if (profile == OptimizationProfile.AGGRESSIVE
                || snapshot.usagePercent() >= Math.min(95, config.getWarningThresholdPercent() + 10)) {
            client.particleEngine.clearParticles();
            this.particleClearCount++;
            particlesCleared = true;
        }

        if (!mapsReset && !particlesCleared) {
            return;
        }

        this.pressureCleanupCount++;
        this.lastPressureCleanupEpochMillis = now;
        updateLastCleanup(now, particlesCleared ? "memory pressure: maps + particles" : "memory pressure: maps");
        metricsTracker.sampleNow();

        Memboost.LOGGER.info(
                "[MemBoost] Pressure cleanup ran at {}% heap usage with profile {}.",
                snapshot.usagePercent(),
                profile.getId()
        );
    }

    public void onWorldChanged(Minecraft client, ClientLevel previousLevel, ClientLevel newLevel, MemoryMetricsTracker metricsTracker) {
        if (previousLevel == null || previousLevel == newLevel) {
            return;
        }

        resetMapTextures(client);
        this.worldChangeCleanupCount++;

        String reason = newLevel != null && previousLevel.dimension() != newLevel.dimension()
                ? "dimension change"
                : "world reload";
        updateLastCleanup(System.currentTimeMillis(), reason);
        metricsTracker.sampleNow();
    }

    public void onDisconnect(Minecraft client, MemoryMetricsTracker metricsTracker) {
        resetMapTextures(client);
        this.disconnectCleanupCount++;
        updateLastCleanup(System.currentTimeMillis(), "disconnect");
        metricsTracker.sampleNow();
    }

    public CleanupStatsSnapshot snapshot(Minecraft client) {
        return new CleanupStatsSnapshot(
                this.pressureCleanupCount,
                this.worldChangeCleanupCount,
                this.disconnectCleanupCount,
                this.mapTextureResetCount,
                this.particleClearCount,
                this.lastCleanupEpochMillis,
                this.lastCleanupReason,
                loadedChunks(client)
        );
    }

    private boolean resetMapTextures(Minecraft client) {
        client.getMapTextureManager().resetData();
        this.mapTextureResetCount++;
        return true;
    }

    private void updateLastCleanup(long epochMillis, String reason) {
        this.lastCleanupEpochMillis = epochMillis;
        this.lastCleanupReason = reason;
    }

    private int loadedChunks(Minecraft client) {
        return client.level == null ? 0 : client.level.getChunkSource().getLoadedChunksCount();
    }
}
