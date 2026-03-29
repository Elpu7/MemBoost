package dev.elpu7.memboost.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.elpu7.memboost.Memboost;
import net.fabricmc.loader.api.FabricLoader;

public final class MemBoostConfigManager {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Path path = FabricLoader.getInstance().getConfigDir().resolve(Memboost.MOD_ID + ".json");
    private MemBoostConfig config = new MemBoostConfig();

    public MemBoostConfig getConfig() {
        return this.config;
    }

    public void load() {
        if (Files.notExists(this.path)) {
            this.config = new MemBoostConfig();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(this.path, StandardCharsets.UTF_8)) {
            MemBoostConfig loadedConfig = GSON.fromJson(reader, MemBoostConfig.class);
            this.config = loadedConfig == null ? new MemBoostConfig() : loadedConfig;
            this.config.sanitize();
        } catch (IOException | RuntimeException exception) {
            Memboost.LOGGER.warn("[MemBoost] Failed to read config {}, using defaults.", this.path, exception);
            this.config = new MemBoostConfig();
        }

        save();
    }

    public void save() {
        this.config.sanitize();

        try {
            Files.createDirectories(this.path.getParent());

            try (Writer writer = Files.newBufferedWriter(
                    this.path,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            )) {
                GSON.toJson(this.config, writer);
            }
        } catch (IOException exception) {
            Memboost.LOGGER.error("[MemBoost] Failed to write config {}.", this.path, exception);
        }
    }
}
