package com.ingenium.client;

import com.ingenium.config.IngeniumYaclScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class IngeniumModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> IngeniumYaclScreen.create(parent);
    }
}
