package com.ingenium.compat;

import net.fabricmc.loader.api.FabricLoader;

public final class ModDetect {
    private static final boolean SODIUM = FabricLoader.getInstance().isModLoaded("sodium");
    private static final boolean LITHIUM = FabricLoader.getInstance().isModLoaded("lithium");
    private static final boolean KRYPTON = FabricLoader.getInstance().isModLoaded("krypton");
    private static final boolean C2ME = FabricLoader.getInstance().isModLoaded("c2me");
    private static final boolean IRIS = FabricLoader.getInstance().isModLoaded("iris");

    private ModDetect() {
    }

    public static boolean isSodiumLoaded() {
        return SODIUM;
    }

    public static boolean isLithiumLoaded() {
        return LITHIUM;
    }

    public static boolean isKryptonLoaded() {
        return KRYPTON;
    }

    public static boolean isC2MELoaded() {
        return C2ME;
    }

    public static boolean isIrisLoaded() {
        return IRIS;
    }

    // Compatibility with Section 10.1 naming
    public static boolean sodiumLoaded() {
        return SODIUM;
    }
}
