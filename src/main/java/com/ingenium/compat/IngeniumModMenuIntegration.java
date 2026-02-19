package com.ingenium.compat;

import com.ingenium.config.IngeniumConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class IngeniumModMenuIntegration implements ModMenuApi {
 
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // ModMenu calls this when the user clicks "Config" in the mod list.
        // We delegate to our YACL builder.
        return IngeniumConfigScreen::create;
    }
}
