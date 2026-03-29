package dev.elpu7.memboost.client.gui;

import dev.elpu7.memboost.client.MemboostClient;
import dev.elpu7.memboost.client.MemoryMetricsSnapshot;
import dev.elpu7.memboost.client.cleanup.CleanupStatsSnapshot;
import dev.elpu7.memboost.config.MemBoostConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MemBoostConfigScreen extends Screen {

    private final Screen parent;

    public MemBoostConfigScreen(Screen parent) {
        super(Component.literal("MemBoost Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MemboostClient.getMetricsTracker().sampleNow();

        int buttonWidth = 220;
        int buttonX = (this.width - buttonWidth) / 2;
        int y = this.height / 4 + 36;

        this.addRenderableWidget(Button.builder(profileMessage(), button -> {
            config().cycleProfile();
            saveConfig();
            button.setMessage(profileMessage());
        }).bounds(buttonX, y, buttonWidth, 20).build());

        y += 24;
        this.addRenderableWidget(Button.builder(hudMessage(), button -> {
            config().setHudEnabled(!config().isHudEnabled());
            saveConfig();
            button.setMessage(hudMessage());
        }).bounds(buttonX, y, buttonWidth, 20).build());

        y += 24;
        this.addRenderableWidget(Button.builder(sampleIntervalMessage(), button -> {
            config().cycleSampleIntervalTicks();
            saveConfig();
            button.setMessage(sampleIntervalMessage());
        }).bounds(buttonX, y, buttonWidth, 20).build());

        y += 24;
        this.addRenderableWidget(Button.builder(warningThresholdMessage(), button -> {
            config().cycleWarningThresholdPercent();
            saveConfig();
            button.setMessage(warningThresholdMessage());
        }).bounds(buttonX, y, buttonWidth, 20).build());

        y += 28;
        this.addRenderableWidget(Button.builder(Component.literal("Reset Peak Usage"), button -> {
            MemboostClient.getMetricsTracker().sampleNow();
            MemboostClient.getMetricsTracker().resetPeak();
        }).bounds(buttonX, y, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.onClose())
                .bounds(buttonX, this.height - 30, buttonWidth, 20)
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
        int centerX = this.width / 2;
        int infoY = 24;

        drawCenteredString(graphics, this.title.getString(), centerX, infoY, 0xFFFFFFFF);
        drawCenteredString(graphics, "Current: " + snapshot.usedMiB() + " / " + snapshot.maxMiB() + " MiB (" + snapshot.usagePercent() + "%)", centerX, infoY + 14, 0xFFFFFFFF);
        drawCenteredString(graphics, "Committed: " + snapshot.committedMiB() + " MiB | Peak: " + snapshot.peakUsedMiB() + " MiB", centerX, infoY + 28, 0xFFB8C0CC);
        drawCenteredString(graphics, "Samples: " + snapshot.sampleCount() + " | Chunks: " + cleanup.loadedChunks(), centerX, infoY + 42, 0xFFB8C0CC);
        drawCenteredString(graphics, "Chunk radius: " + cleanup.activeChunkRadius() + " / " + cleanup.serverChunkRadius() + " | Pressure: " + cleanup.chunkPressureActivationCount(), centerX, infoY + 56, 0xFFB8C0CC);
        drawCenteredString(graphics, "Cleanups: " + cleanup.totalCleanupCount() + " | Last: " + cleanup.describeLastCleanup(), centerX, infoY + 70, 0xFFB8C0CC);
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

    private Component hudMessage() {
        return Component.literal("Metrics HUD: " + (config().isHudEnabled() ? "On" : "Off"));
    }

    private Component sampleIntervalMessage() {
        return Component.literal("Sample Interval: " + config().getSampleIntervalTicks() + " ticks");
    }

    private Component warningThresholdMessage() {
        return Component.literal("Warning Threshold: " + config().getWarningThresholdPercent() + "%");
    }
}
