package dev.elpu7.memboost.client;

import java.util.Objects;

import dev.elpu7.memboost.Memboost;
import dev.elpu7.memboost.client.command.MemBoostClientCommands;
import dev.elpu7.memboost.client.cleanup.MemBoostCleanupCoordinator;
import dev.elpu7.memboost.client.gui.MemBoostConfigScreen;
import dev.elpu7.memboost.client.hud.MemBoostHud;
import dev.elpu7.memboost.config.MemBoostConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

public class MemboostClient implements ClientModInitializer {

    private static MemBoostConfigManager configManager;
    private static MemoryMetricsTracker metricsTracker;
    private static MemBoostCleanupCoordinator cleanupCoordinator;

    @Override
    public void onInitializeClient() {
        configManager = new MemBoostConfigManager();
        configManager.load();

        metricsTracker = new MemoryMetricsTracker();
        metricsTracker.sampleNow();
        cleanupCoordinator = new MemBoostCleanupCoordinator();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            metricsTracker.tick(configManager.getConfig());
            cleanupCoordinator.tick(client, configManager.getConfig(), metricsTracker);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> configManager.save());
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> cleanupCoordinator.onDisconnect(client, metricsTracker));
        ClientCommandRegistrationCallback.EVENT.register(MemBoostClientCommands::register);

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(Memboost.MOD_ID, "memory_metrics"),
                MemBoostHud::render
        );

        Memboost.LOGGER.info("[MemBoost] Client initialized.");
    }

    public static MemBoostConfigManager getConfigManager() {
        return Objects.requireNonNull(configManager, "MemBoost config manager has not been initialized.");
    }

    public static MemoryMetricsTracker getMetricsTracker() {
        return Objects.requireNonNull(metricsTracker, "MemBoost metrics tracker has not been initialized.");
    }

    public static MemBoostCleanupCoordinator getCleanupCoordinator() {
        return Objects.requireNonNull(cleanupCoordinator, "MemBoost cleanup coordinator has not been initialized.");
    }

    public static void openConfigScreen(Screen parent) {
        Minecraft.getInstance().setScreen(new MemBoostConfigScreen(parent));
    }

    public static void handleWorldChanged(ClientLevel previousLevel, ClientLevel newLevel) {
        if (cleanupCoordinator == null || metricsTracker == null) {
            return;
        }

        cleanupCoordinator.onWorldChanged(Minecraft.getInstance(), previousLevel, newLevel, metricsTracker);
    }
}
