package dev.elpu7.memboost;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Memboost implements ModInitializer {

    public static final String MOD_ID = "memboost";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[MemBoost] Initialized.");
    }
}
