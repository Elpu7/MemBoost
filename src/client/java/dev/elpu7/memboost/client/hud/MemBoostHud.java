package dev.elpu7.memboost.client.hud;

import java.util.List;

import dev.elpu7.memboost.client.MemboostClient;
import dev.elpu7.memboost.client.MemoryMetricsSnapshot;
import dev.elpu7.memboost.client.cleanup.CleanupStatsSnapshot;
import dev.elpu7.memboost.client.network.PacketStatsSnapshot;
import dev.elpu7.memboost.config.MemBoostConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class MemBoostHud {

    private static final int BACKGROUND_COLOR = 0x66000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int STABLE_COLOR = 0xFF9EE493;
    private static final int WARNING_COLOR = 0xFFFFC857;
    private static final int CRITICAL_COLOR = 0xFFFF6B6B;

    private MemBoostHud() {
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        MemBoostConfig config = MemboostClient.getConfigManager().getConfig();

        if (!config.isHudEnabled()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        MemoryMetricsSnapshot snapshot = MemboostClient.getMetricsTracker().snapshot();
        CleanupStatsSnapshot cleanup = MemboostClient.getCleanupCoordinator().snapshot(client);
        PacketStatsSnapshot packetStats = MemboostClient.getPacketBurstMonitor().snapshot();
        int effectiveThreshold = MemboostClient.getCleanupCoordinator().effectiveWarningThreshold(client, config);
        List<String> lines = List.of(
                "MemBoost " + snapshot.usedMiB() + " / " + snapshot.maxMiB() + " MiB (" + snapshot.usagePercent() + "%)",
                "Peak " + snapshot.peakUsedMiB() + " MiB | Chunks " + cleanup.loadedChunks(),
                "Radius " + cleanup.activeChunkRadius() + " / " + cleanup.serverChunkRadius() + " | Chunk pressure " + cleanup.chunkPressureActivationCount(),
                "Packets " + packetStats.recentChunkPackets() + "/" + packetStats.recentLightPackets() + "/" + packetStats.recentForgetPackets() + " | Burst " + cleanup.packetBurstCleanupCount(),
                "Freed maps " + cleanup.mapTextureResetCount() + " | particles " + cleanup.particleClearCount() + " | transient " + cleanup.transientStateResetCount(),
                "Cleanups " + cleanup.totalCleanupCount() + " | Reloads " + cleanup.resourceReloadCleanupCount() + " | Last " + cleanup.describeLastCleanup(),
                "Preset " + config.getLastPreset().getDisplayName() + " | Profile " + config.getProfile().getDisplayName(),
                "Alert " + config.getWarningThresholdPercent() + "% -> " + effectiveThreshold + "%"
        );

        int x = 8;
        int y = 8;
        int lineSpacing = client.font.lineHeight + 2;
        int contentHeight = lines.size() * lineSpacing + 2;
        int maxWidth = 0;

        for (String line : lines) {
            maxWidth = Math.max(maxWidth, client.font.width(line));
        }

        graphics.fill(x - 4, y - 4, x + maxWidth + 4, y + contentHeight, BACKGROUND_COLOR);

        for (int i = 0; i < lines.size(); i++) {
            int color = i == 0 ? usageColor(snapshot, config) : TEXT_COLOR;
            graphics.text(client.font, lines.get(i), x, y + i * lineSpacing, color, true);
        }
    }

    private static int usageColor(MemoryMetricsSnapshot snapshot, MemBoostConfig config) {
        int usagePercent = snapshot.usagePercent();

        if (usagePercent >= config.getWarningThresholdPercent()) {
            return CRITICAL_COLOR;
        }

        if (usagePercent >= Math.max(50, config.getWarningThresholdPercent() - 10)) {
            return WARNING_COLOR;
        }

        return STABLE_COLOR;
    }
}
