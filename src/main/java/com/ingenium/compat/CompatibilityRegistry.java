package com.ingenium.compat;

import net.fabricmc.loader.api.FabricLoader;

public final class CompatibilityRegistry {
    public static volatile boolean C2ME_LOADED;

    private CompatibilityRegistry() {
    }

    public static void init() {
        C2ME_LOADED = FabricLoader.getInstance().isModLoaded("c2me");
    }
}
