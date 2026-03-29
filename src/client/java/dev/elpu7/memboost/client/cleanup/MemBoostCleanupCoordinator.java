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
    private static final int MIN_CHUNK_RADIUS = 2;

    private long pressureCleanupCount;
    private long worldChangeCleanupCount;
    private long disconnectCleanupCount;
    private long mapTextureResetCount;
    private long particleClearCount;
    private long chunkPressureActivationCount;
    private long lastCleanupEpochMillis;
    private long lastPressureCleanupEpochMillis;
    private String lastCleanupReason = "none";
    private int serverChunkRadius = -1;
    private int activeChunkRadius = -1;

    public void tick(Minecraft client, MemBoostConfig config, MemoryMetricsTracker metricsTracker) {
        OptimizationProfile profile = config.getProfile();
        updateChunkPressureMode(client, config, metricsTracker.snapshot());

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
        if (newLevel != null && this.serverChunkRadius > 0) {
            applyChunkRadius(newLevel, this.serverChunkRadius);
        }
        metricsTracker.sampleNow();
    }

    public void onDisconnect(Minecraft client, MemoryMetricsTracker metricsTracker) {
        resetMapTextures(client);
        this.disconnectCleanupCount++;
        updateLastCleanup(System.currentTimeMillis(), "disconnect");
        this.activeChunkRadius = -1;
        metricsTracker.sampleNow();
    }

    public void updateServerChunkRadius(Minecraft client, int radius) {
        this.serverChunkRadius = Math.max(MIN_CHUNK_RADIUS, radius);

        if (client.level == null) {
            this.activeChunkRadius = this.serverChunkRadius;
            return;
        }

        if (this.activeChunkRadius < 0 || this.activeChunkRadius >= this.serverChunkRadius) {
            applyChunkRadius(client.level, this.serverChunkRadius);
        }
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
                loadedChunks(client),
                this.serverChunkRadius,
                this.activeChunkRadius,
                this.chunkPressureActivationCount
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

    private void updateChunkPressureMode(Minecraft client, MemBoostConfig config, MemoryMetricsSnapshot snapshot) {
        if (client.level == null || this.serverChunkRadius < 0) {
            return;
        }

        OptimizationProfile profile = config.getProfile();

        if (profile == OptimizationProfile.SAFE) {
            restoreChunkRadius(client.level);
            return;
        }

        int threshold = config.getWarningThresholdPercent();
        int usage = snapshot.usagePercent();

        if (usage >= threshold) {
            int reduction = profile == OptimizationProfile.AGGRESSIVE ? 4 : 2;

            if (usage >= Math.min(95, threshold + 10)) {
                reduction += profile == OptimizationProfile.AGGRESSIVE ? 2 : 1;
            }

            int targetRadius = Math.max(MIN_CHUNK_RADIUS, this.serverChunkRadius - reduction);

            if (targetRadius < this.serverChunkRadius && targetRadius != this.activeChunkRadius) {
                applyChunkRadius(client.level, targetRadius);
                this.chunkPressureActivationCount++;
                updateLastCleanup(System.currentTimeMillis(), "chunk pressure mode");
                Memboost.LOGGER.info(
                        "[MemBoost] Chunk pressure mode lowered client chunk radius from {} to {} at {}% heap usage.",
                        this.serverChunkRadius,
                        targetRadius,
                        usage
                );
            }
            return;
        }

        if (usage <= Math.max(35, threshold - 10)) {
            restoreChunkRadius(client.level);
        }
    }

    private void restoreChunkRadius(ClientLevel level) {
        if (this.serverChunkRadius < 0) {
            return;
        }

        if (this.activeChunkRadius != this.serverChunkRadius) {
            applyChunkRadius(level, this.serverChunkRadius);
        }
    }

    private void applyChunkRadius(ClientLevel level, int radius) {
        level.getChunkSource().updateViewRadius(radius);
        this.activeChunkRadius = radius;
    }

    private int loadedChunks(Minecraft client) {
        return client.level == null ? 0 : client.level.getChunkSource().getLoadedChunksCount();
    }
}
