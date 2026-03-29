package dev.elpu7.memboost.client.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.elpu7.memboost.client.gui.MemBoostConfigScreen;

public final class MemBoostModMenuApi implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return MemBoostConfigScreen::new;
    }
}
