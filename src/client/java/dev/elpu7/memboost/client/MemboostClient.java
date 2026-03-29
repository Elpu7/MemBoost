package dev.elpu7.memboost.client;

import java.util.Objects;

import dev.elpu7.memboost.Memboost;
import dev.elpu7.memboost.client.command.MemBoostClientCommands;
import dev.elpu7.memboost.client.gui.MemBoostConfigScreen;
import dev.elpu7.memboost.client.hud.MemBoostHud;
import dev.elpu7.memboost.config.MemBoostConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

public class MemboostClient implements ClientModInitializer {

    private static MemBoostConfigManager configManager;
    private static MemoryMetricsTracker metricsTracker;

    @Override
    public void onInitializeClient() {
        configManager = new MemBoostConfigManager();
        configManager.load();

        metricsTracker = new MemoryMetricsTracker();
        metricsTracker.sampleNow();

        ClientTickEvents.END_CLIENT_TICK.register(client -> metricsTracker.tick(configManager.getConfig()));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> configManager.save());
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

    public static void openConfigScreen(Screen parent) {
        Minecraft.getInstance().setScreen(new MemBoostConfigScreen(parent));
    }
}
