package dev.elpu7.memboost.client.gui;

import dev.elpu7.memboost.client.MemboostClient;
import dev.elpu7.memboost.client.MemoryMetricsSnapshot;
import dev.elpu7.memboost.client.cleanup.CleanupStatsSnapshot;
import dev.elpu7.memboost.client.network.PacketStatsSnapshot;
import dev.elpu7.memboost.config.MemBoostConfig;
import dev.elpu7.memboost.config.MemBoostPreset;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MemBoostConfigScreen extends Screen {

    private static final int INFO_TOP = 22;
    private static final int INFO_LINE_SPACING = 14;
    private static final int INFO_LINES = 9;

    private final Screen parent;

    public MemBoostConfigScreen(Screen parent) {
        super(Component.literal("MemBoost Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MemboostClient.getMetricsTracker().sampleNow();

        int buttonWidth = 152;
        int buttonGap = 8;
        int leftX = this.width / 2 - buttonWidth - buttonGap / 2;
        int rightX = this.width / 2 + buttonGap / 2;
        int y = Math.max(this.height / 4 + 34, INFO_TOP + INFO_LINE_SPACING * INFO_LINES + 14);

        this.addRenderableWidget(Button.builder(presetMessage(), button -> {
            config().applyPreset(nextPreset());
            saveConfig();
            this.rebuildWidgets();
        }).bounds(leftX, y, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(profileMessage(), button -> {
            config().cycleProfile();
            saveConfig();
            button.setMessage(profileMessage());
        }).bounds(rightX, y, buttonWidth, 20).build());

        y += 24;
        this.addRenderableWidget(Button.builder(hudMessage(), button -> {
            config().setHudEnabled(!config().isHudEnabled());
            saveConfig();
            button.setMessage(hudMessage());
        }).bounds(leftX, y, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(debugMessage(), button -> {
            config().setDebugLoggingEnabled(!config().isDebugLoggingEnabled());
            saveConfig();
            button.setMessage(debugMessage());
        }).bounds(rightX, y, buttonWidth, 20).build());

        y += 24;
        this.addRenderableWidget(Button.builder(sampleIntervalMessage(), button -> {
            config().cycleSampleIntervalTicks();
            saveConfig();
            button.setMessage(sampleIntervalMessage());
        }).bounds(leftX, y, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(warningThresholdMessage(), button -> {
            config().cycleWarningThresholdPercent();
            saveConfig();
            button.setMessage(warningThresholdMessage());
        }).bounds(rightX, y, buttonWidth, 20).build());

        y += 28;
        this.addRenderableWidget(Button.builder(Component.literal("Reset Peak Usage"), button -> {
            MemboostClient.getMetricsTracker().sampleNow();
            MemboostClient.getMetricsTracker().resetPeak();
        }).bounds(this.width / 2 - 110, y, 220, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.onClose())
                .bounds(this.width / 2 - 110, this.height - 28, 220, 20)
                .build());
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        MemoryMetricsSnapshot snapshot = MemboostClient.getMetricsTracker().snapshot();
        CleanupStatsSnapshot cleanup = MemboostClient.getCleanupCoordinator().snapshot(this.minecraft);
        PacketStatsSnapshot packetStats = MemboostClient.getPacketBurstMonitor().snapshot();
        int effectiveThreshold = MemboostClient.getCleanupCoordinator().effectiveWarningThreshold(this.minecraft, config());
        int centerX = this.width / 2;
        int infoY = INFO_TOP;

        drawCenteredString(graphics, this.title.getString(), centerX, infoY, 0xFFFFFFFF);
        drawCenteredString(graphics, "Preset: " + config().getLastPreset().getDisplayName(), centerX, infoY + 14, 0xFFFFFFFF);
        drawCenteredString(graphics, "Current: " + snapshot.usedMiB() + " / " + snapshot.maxMiB() + " MiB (" + snapshot.usagePercent() + "%)", centerX, infoY + 28, 0xFFFFFFFF);
        drawCenteredString(graphics, "Committed: " + snapshot.committedMiB() + " MiB | Peak: " + snapshot.peakUsedMiB() + " MiB", centerX, infoY + 42, 0xFFB8C0CC);
        drawCenteredString(graphics, "Samples: " + snapshot.sampleCount() + " | Chunks: " + cleanup.loadedChunks(), centerX, infoY + 56, 0xFFB8C0CC);
        drawCenteredString(graphics, "Alert: " + config().getWarningThresholdPercent() + "% -> effective " + effectiveThreshold + "% | Debug: " + (config().isDebugLoggingEnabled() ? "On" : "Off"), centerX, infoY + 70, 0xFFB8C0CC);
        drawCenteredString(graphics, "Chunk radius: " + cleanup.activeChunkRadius() + " / " + cleanup.serverChunkRadius() + " | Pressure: " + cleanup.chunkPressureActivationCount(), centerX, infoY + 84, 0xFFB8C0CC);
        drawCenteredString(graphics, "Packets/20t: " + packetStats.recentChunkPackets() + " chunk, " + packetStats.recentLightPackets() + " light, " + packetStats.recentForgetPackets() + " forget", centerX, infoY + 98, 0xFFB8C0CC);
        drawCenteredString(graphics, "Cleanups: " + cleanup.totalCleanupCount() + " | Reloads: " + cleanup.resourceReloadCleanupCount() + " | Last: " + cleanup.describeLastCleanup(), centerX, infoY + 112, 0xFFB8C0CC);
        drawCenteredString(graphics, "Freed: maps " + cleanup.mapTextureResetCount() + ", particles " + cleanup.particleClearCount() + ", transient " + cleanup.transientStateResetCount(), centerX, infoY + 126, 0xFFB8C0CC);
    }

    private void drawCenteredString(GuiGraphicsExtractor graphics, String text, int centerX, int y, int color) {
        graphics.centeredText(this.font, text, centerX, y, color);
    }

    private MemBoostConfig config() {
        return MemboostClient.getConfigManager().getConfig();
    }

    private void saveConfig() {
        MemboostClient.getConfigManager().save();
        MemboostClient.getMetricsTracker().sampleNow();
    }

    private Component profileMessage() {
        return Component.literal("Profile: " + config().getProfile().getDisplayName());
    }

    private Component presetMessage() {
        return Component.literal("Preset: " + config().getLastPreset().getDisplayName());
    }

    private Component hudMessage() {
        return Component.literal("HUD: " + (config().isHudEnabled() ? "On" : "Off"));
    }

    private Component debugMessage() {
        return Component.literal("Debug: " + (config().isDebugLoggingEnabled() ? "On" : "Off"));
    }

    private Component sampleIntervalMessage() {
        return Component.literal("Interval: " + config().getSampleIntervalTicks() + " ticks");
    }

    private Component warningThresholdMessage() {
        return Component.literal("Alert: " + config().getWarningThresholdPercent() + "%");
    }

    private MemBoostPreset nextPreset() {
        return switch (config().getLastPreset()) {
            case PLAY -> MemBoostPreset.OBSERVE;
            case OBSERVE -> MemBoostPreset.STRESS;
            case STRESS -> MemBoostPreset.PLAY;
        };
    }
}
