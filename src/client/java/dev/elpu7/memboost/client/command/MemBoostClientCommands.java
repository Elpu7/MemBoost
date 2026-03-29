package dev.elpu7.memboost.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.elpu7.memboost.client.MemboostClient;
import dev.elpu7.memboost.client.MemoryMetricsSnapshot;
import dev.elpu7.memboost.client.cleanup.CleanupStatsSnapshot;
import dev.elpu7.memboost.config.MemBoostConfig;
import dev.elpu7.memboost.config.OptimizationProfile;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class MemBoostClientCommands {

    private MemBoostClientCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(literal("memboost")
                .executes(context -> showOverview(context.getSource()))
                .then(literal("stats").executes(context -> showStats(context.getSource())))
                .then(literal("config").executes(context -> openConfig(context.getSource())))
                .then(literal("resetpeak").executes(context -> resetPeak(context.getSource())))
                .then(literal("hud")
                        .then(literal("on").executes(context -> setHud(context.getSource(), true)))
                        .then(literal("off").executes(context -> setHud(context.getSource(), false))))
                .then(literal("profile")
                        .then(literal("safe").executes(context -> setProfile(context.getSource(), OptimizationProfile.SAFE)))
                        .then(literal("balanced").executes(context -> setProfile(context.getSource(), OptimizationProfile.BALANCED)))
                        .then(literal("aggressive").executes(context -> setProfile(context.getSource(), OptimizationProfile.AGGRESSIVE))))
                .then(literal("interval")
                        .then(argument("ticks", IntegerArgumentType.integer(1, 1200))
                                .executes(context -> setInterval(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "ticks")
                                ))))
                .then(literal("threshold")
                        .then(argument("percent", IntegerArgumentType.integer(50, 95))
                                .executes(context -> setThreshold(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "percent")
                                )))));
    }

    private static int showOverview(FabricClientCommandSource source) {
        int result = showStats(source);
        source.sendFeedback(Component.literal("Commands: /memboost config | /memboost hud <on|off> | /memboost profile <safe|balanced|aggressive>"));
        return result;
    }

    private static int showStats(FabricClientCommandSource source) {
        MemboostClient.getMetricsTracker().sampleNow();

        MemoryMetricsSnapshot snapshot = MemboostClient.getMetricsTracker().snapshot();
        MemBoostConfig config = MemboostClient.getConfigManager().getConfig();
        CleanupStatsSnapshot cleanup = MemboostClient.getCleanupCoordinator().snapshot(Minecraft.getInstance());

        source.sendFeedback(Component.literal("MemBoost stats"));
        source.sendFeedback(Component.literal("Used: " + snapshot.usedMiB() + " MiB / " + snapshot.maxMiB() + " MiB (" + snapshot.usagePercent() + "%)"));
        source.sendFeedback(Component.literal("Committed: " + snapshot.committedMiB() + " MiB | Peak: " + snapshot.peakUsedMiB() + " MiB"));
        source.sendFeedback(Component.literal("Samples: " + snapshot.sampleCount() + " | Chunks: " + cleanup.loadedChunks() + " | HUD: " + (config.isHudEnabled() ? "on" : "off")));
        source.sendFeedback(Component.literal("Profile: " + config.getProfile().getDisplayName() + " | Interval: " + config.getSampleIntervalTicks() + " ticks | Alert: " + config.getWarningThresholdPercent() + "%"));
        source.sendFeedback(Component.literal("Chunk radius: active " + cleanup.activeChunkRadius() + " / server " + cleanup.serverChunkRadius() + " | Pressure activations: " + cleanup.chunkPressureActivationCount()));
        source.sendFeedback(Component.literal("Cleanups: " + cleanup.totalCleanupCount() + " (pressure " + cleanup.pressureCleanupCount() + ", world " + cleanup.worldChangeCleanupCount() + ", disconnect " + cleanup.disconnectCleanupCount() + ")"));
        source.sendFeedback(Component.literal("Resources freed: maps " + cleanup.mapTextureResetCount() + ", particles " + cleanup.particleClearCount() + " | Last: " + cleanup.describeLastCleanup()));
        return snapshot.usagePercent();
    }

    private static int openConfig(FabricClientCommandSource source) {
        MemboostClient.openConfigScreen(null);
        source.sendFeedback(Component.literal("Opened MemBoost settings."));
        return 1;
    }

    private static int resetPeak(FabricClientCommandSource source) {
        MemboostClient.getMetricsTracker().sampleNow();
        MemboostClient.getMetricsTracker().resetPeak();
        source.sendFeedback(Component.literal("MemBoost peak usage has been reset."));
        return 1;
    }

    private static int setHud(FabricClientCommandSource source, boolean enabled) {
        MemBoostConfig config = MemboostClient.getConfigManager().getConfig();
        config.setHudEnabled(enabled);
        MemboostClient.getConfigManager().save();
        source.sendFeedback(Component.literal("MemBoost HUD " + (enabled ? "enabled." : "disabled.")));
        return enabled ? 1 : 0;
    }

    private static int setProfile(FabricClientCommandSource source, OptimizationProfile profile) {
        MemBoostConfig config = MemboostClient.getConfigManager().getConfig();
        config.setProfile(profile);
        MemboostClient.getConfigManager().save();
        source.sendFeedback(Component.literal("MemBoost profile set to " + profile.getDisplayName() + "."));
        return 1;
    }

    private static int setInterval(FabricClientCommandSource source, int ticks) {
        MemBoostConfig config = MemboostClient.getConfigManager().getConfig();
        config.setSampleIntervalTicks(ticks);
        MemboostClient.getConfigManager().save();
        source.sendFeedback(Component.literal("MemBoost sample interval set to " + config.getSampleIntervalTicks() + " ticks."));
        return config.getSampleIntervalTicks();
    }

    private static int setThreshold(FabricClientCommandSource source, int percent) {
        MemBoostConfig config = MemboostClient.getConfigManager().getConfig();
        config.setWarningThresholdPercent(percent);
        MemboostClient.getConfigManager().save();
        source.sendFeedback(Component.literal("MemBoost warning threshold set to " + config.getWarningThresholdPercent() + "%."));
        return config.getWarningThresholdPercent();
    }
}
