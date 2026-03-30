package dev.elpu7.memboost.client.cleanup;

import dev.elpu7.memboost.Memboost;
import dev.elpu7.memboost.client.MemoryMetricsSnapshot;
import dev.elpu7.memboost.client.MemoryMetricsTracker;
import dev.elpu7.memboost.client.network.PacketBurstMonitor;
import dev.elpu7.memboost.client.network.PacketStatsSnapshot;
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
    private long resourceReloadCleanupCount;
    private long mapTextureResetCount;
    private long particleClearCount;
    private long transientStateResetCount;
    private long chunkPressureActivationCount;
    private long packetBurstCleanupCount;
    private long lastCleanupEpochMillis;
    private long lastPressureCleanupEpochMillis;
    private String lastCleanupReason = "none";
    private int serverChunkRadius = -1;
    private int activeChunkRadius = -1;
    private int lastSuggestedChunkRadius = -1;

    public int effectiveWarningThreshold(Minecraft client, MemBoostConfig config) {
        return effectiveWarningThresholdPercent(client, config);
    }

    public void tick(Minecraft client, MemBoostConfig config, MemoryMetricsTracker metricsTracker, PacketBurstMonitor packetBurstMonitor) {
        OptimizationProfile profile = config.getProfile();
        updateChunkPressureMode(client, config, metricsTracker.snapshot());

        if (profile == OptimizationProfile.SAFE) {
            return;
        }

        MemoryMetricsSnapshot snapshot = metricsTracker.snapshot();
        PacketStatsSnapshot packetStats = packetBurstMonitor.snapshot();

        if (handlePacketBurstPressure(client, config, metricsTracker, packetBurstMonitor, snapshot, packetStats)) {
            return;
        }

        int warningThreshold = effectiveWarningThresholdPercent(client, config);
        if (snapshot.usagePercent() < warningThreshold) {
            return;
        }

        long now = System.currentTimeMillis();
        long cooldownMs = pressureCooldownMillis(client, profile);

        if (now - this.lastPressureCleanupEpochMillis < cooldownMs) {
            return;
        }

        boolean mapsReset = resetMapTextures(client);
        boolean particlesCleared = false;

        if (shouldClearParticlesForPressure(client, config, snapshot.usagePercent(), warningThreshold)) {
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

        debugLog(config, "[MemBoost] Pressure cleanup ran at {}% heap usage with profile {} (effective threshold {}%).",
                snapshot.usagePercent(),
                profile.getId(),
                warningThreshold);
    }

    public void onWorldChanged(Minecraft client, ClientLevel previousLevel, ClientLevel newLevel, MemoryMetricsTracker metricsTracker, PacketBurstMonitor packetBurstMonitor) {
        if (previousLevel == null || previousLevel == newLevel) {
            return;
        }

        resetMapTextures(client);
        resetTransientClientState(client, packetBurstMonitor);
        this.worldChangeCleanupCount++;

        String reason = newLevel != null && previousLevel.dimension() != newLevel.dimension()
                ? "dimension change"
                : "world reload";
        updateLastCleanup(System.currentTimeMillis(), reason);
        this.activeChunkRadius = this.serverChunkRadius;
        this.lastSuggestedChunkRadius = -1;
        metricsTracker.sampleNow();
    }

    public void onDisconnect(Minecraft client, MemoryMetricsTracker metricsTracker, PacketBurstMonitor packetBurstMonitor) {
        resetMapTextures(client);
        resetTransientClientState(client, packetBurstMonitor);
        this.disconnectCleanupCount++;
        updateLastCleanup(System.currentTimeMillis(), "disconnect");
        this.activeChunkRadius = -1;
        this.lastSuggestedChunkRadius = -1;
        metricsTracker.sampleNow();
    }

    public void onResourceReload(Minecraft client, MemBoostConfig config, MemoryMetricsTracker metricsTracker, PacketBurstMonitor packetBurstMonitor) {
        resetMapTextures(client);
        resetTransientClientState(client, packetBurstMonitor);
        this.resourceReloadCleanupCount++;
        updateLastCleanup(System.currentTimeMillis(), "resource reload");
        metricsTracker.sampleNow();
        debugLog(config, "[MemBoost] Ran resource reload cleanup.");
    }

    public void updateServerChunkRadius(Minecraft client, int radius) {
        this.serverChunkRadius = Math.max(MIN_CHUNK_RADIUS, radius);
        this.activeChunkRadius = this.serverChunkRadius;
        this.lastSuggestedChunkRadius = -1;
    }

    public CleanupStatsSnapshot snapshot(Minecraft client) {
        return new CleanupStatsSnapshot(
                this.pressureCleanupCount,
                this.worldChangeCleanupCount,
                this.disconnectCleanupCount,
                this.resourceReloadCleanupCount,
                this.mapTextureResetCount,
                this.particleClearCount,
                this.transientStateResetCount,
                this.lastCleanupEpochMillis,
                this.lastCleanupReason,
                loadedChunks(client),
                this.serverChunkRadius,
                this.activeChunkRadius,
                this.chunkPressureActivationCount,
                this.packetBurstCleanupCount
        );
    }

    private boolean resetMapTextures(Minecraft client) {
        client.getMapTextureManager().resetData();
        this.mapTextureResetCount++;
        return true;
    }

    private void resetTransientClientState(Minecraft client, PacketBurstMonitor packetBurstMonitor) {
        packetBurstMonitor.resetWindow();

        if (client.particleEngine != null) {
            client.particleEngine.clearParticles();
            this.particleClearCount++;
        }

        this.transientStateResetCount++;
    }

    private void updateLastCleanup(long epochMillis, String reason) {
        this.lastCleanupEpochMillis = epochMillis;
        this.lastCleanupReason = reason;
    }

    private void updateChunkPressureMode(Minecraft client, MemBoostConfig config, MemoryMetricsSnapshot snapshot) {
        if (client.level == null || this.serverChunkRadius < 0) {
            return;
        }

        this.activeChunkRadius = this.serverChunkRadius;

        OptimizationProfile profile = config.getProfile();

        if (profile == OptimizationProfile.SAFE) {
            return;
        }

        int threshold = effectiveWarningThresholdPercent(client, config);
        int usage = snapshot.usagePercent();

        if (usage < threshold) {
            this.lastSuggestedChunkRadius = -1;
            return;
        }

        int reduction = profile == OptimizationProfile.AGGRESSIVE ? 4 : 2;

        if (usage >= Math.min(95, threshold + 10)) {
            reduction += profile == OptimizationProfile.AGGRESSIVE ? 2 : 1;
        }

        int suggestedRadius = Math.max(MIN_CHUNK_RADIUS, this.serverChunkRadius - reduction);

        if (suggestedRadius >= this.serverChunkRadius) {
            this.lastSuggestedChunkRadius = -1;
            return;
        }

        if (suggestedRadius != this.lastSuggestedChunkRadius) {
            this.lastSuggestedChunkRadius = suggestedRadius;
            this.chunkPressureActivationCount++;
            debugLog(config,
                    "[MemBoost] Chunk pressure mode detected pressure at {}% heap usage. Suggested radius {} from server radius {}.",
                    usage,
                    suggestedRadius,
                    this.serverChunkRadius
            );
        }
    }

    private boolean handlePacketBurstPressure(
            Minecraft client,
            MemBoostConfig config,
            MemoryMetricsTracker metricsTracker,
            PacketBurstMonitor packetBurstMonitor,
            MemoryMetricsSnapshot snapshot,
            PacketStatsSnapshot packetStats
    ) {
        if (client.level == null) {
            return false;
        }

        int warningThreshold = effectiveWarningThresholdPercent(client, config);
        if (snapshot.usagePercent() < Math.max(55, warningThreshold - 3)) {
            return false;
        }

        boolean integratedServer = isIntegratedServer(client);
        boolean chunkBurst = packetStats.recentChunkPackets() >= packetChunkBurstThreshold(config.getProfile(), integratedServer);
        boolean lightBurst = packetStats.recentLightPackets() >= packetLightBurstThreshold(config.getProfile(), integratedServer);
        boolean unloadBurst = packetStats.recentForgetPackets() >= packetForgetBurstThreshold(config.getProfile(), integratedServer);

        if (!chunkBurst && !lightBurst && !unloadBurst) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - this.lastPressureCleanupEpochMillis < packetBurstCooldownMillis(config.getProfile(), integratedServer)) {
            return false;
        }

        resetMapTextures(client);

        if (shouldClearParticlesForPacketBurst(client, config, snapshot.usagePercent(), warningThreshold, chunkBurst)) {
            client.particleEngine.clearParticles();
            this.particleClearCount++;
        }

        this.packetBurstCleanupCount++;
        this.pressureCleanupCount++;
        this.lastPressureCleanupEpochMillis = now;
        packetBurstMonitor.markBurstActivation();
        updateLastCleanup(now, "packet burst pressure");
        metricsTracker.sampleNow();
        debugLog(config,
                "[MemBoost] Packet burst cleanup ran. recent chunk={}, light={}, forget={}.",
                packetStats.recentChunkPackets(),
                packetStats.recentLightPackets(),
                packetStats.recentForgetPackets()
        );
        return true;
    }

    private int loadedChunks(Minecraft client) {
        return client.level == null ? 0 : client.level.getChunkSource().getLoadedChunksCount();
    }

    private boolean shouldClearParticlesForPressure(Minecraft client, MemBoostConfig config, int usagePercent, int warningThreshold) {
        if (config.getProfile() == OptimizationProfile.SAFE) {
            return false;
        }

        int particleThreshold = isIntegratedServer(client)
                ? Math.min(98, warningThreshold + 12)
                : Math.min(96, warningThreshold + (config.getProfile() == OptimizationProfile.AGGRESSIVE ? 6 : 10));
        return usagePercent >= particleThreshold;
    }

    private boolean shouldClearParticlesForPacketBurst(Minecraft client, MemBoostConfig config, int usagePercent, int warningThreshold, boolean chunkBurst) {
        if (config.getProfile() == OptimizationProfile.AGGRESSIVE) {
            int particleThreshold = isIntegratedServer(client) ? Math.min(98, warningThreshold + 10) : Math.min(95, warningThreshold + 5);
            return usagePercent >= particleThreshold;
        }

        return chunkBurst && usagePercent >= Math.min(98, warningThreshold + 8);
    }

    private int effectiveWarningThresholdPercent(Minecraft client, MemBoostConfig config) {
        int threshold = config.getWarningThresholdPercent();
        if (!isIntegratedServer(client)) {
            return threshold;
        }

        return switch (config.getProfile()) {
            case SAFE -> Math.min(95, threshold + 8);
            case BALANCED -> Math.min(95, threshold + 6);
            case AGGRESSIVE -> Math.min(95, threshold + 10);
        };
    }

    private long pressureCooldownMillis(Minecraft client, OptimizationProfile profile) {
        boolean integratedServer = isIntegratedServer(client);
        return switch (profile) {
            case SAFE -> Long.MAX_VALUE;
            case BALANCED -> integratedServer ? 22_500L : BALANCED_PRESSURE_COOLDOWN_MS;
            case AGGRESSIVE -> integratedServer ? 15_000L : AGGRESSIVE_PRESSURE_COOLDOWN_MS;
        };
    }

    private long packetBurstCooldownMillis(OptimizationProfile profile, boolean integratedServer) {
        return switch (profile) {
            case SAFE -> Long.MAX_VALUE;
            case BALANCED -> integratedServer ? 6_000L : 3_500L;
            case AGGRESSIVE -> integratedServer ? 5_000L : 2_500L;
        };
    }

    private int packetChunkBurstThreshold(OptimizationProfile profile, boolean integratedServer) {
        return switch (profile) {
            case SAFE -> Integer.MAX_VALUE;
            case BALANCED -> integratedServer ? 40 : 20;
            case AGGRESSIVE -> integratedServer ? 28 : 12;
        };
    }

    private int packetLightBurstThreshold(OptimizationProfile profile, boolean integratedServer) {
        return switch (profile) {
            case SAFE -> Integer.MAX_VALUE;
            case BALANCED -> integratedServer ? 64 : 36;
            case AGGRESSIVE -> integratedServer ? 48 : 24;
        };
    }

    private int packetForgetBurstThreshold(OptimizationProfile profile, boolean integratedServer) {
        return switch (profile) {
            case SAFE -> Integer.MAX_VALUE;
            case BALANCED -> integratedServer ? 28 : 16;
            case AGGRESSIVE -> integratedServer ? 20 : 12;
        };
    }

    private boolean isIntegratedServer(Minecraft client) {
        return client != null && client.getSingleplayerServer() != null;
    }

    private void debugLog(MemBoostConfig config, String message, Object... args) {
        if (config.isDebugLoggingEnabled()) {
            Memboost.LOGGER.info(message, args);
        }
    }
}
