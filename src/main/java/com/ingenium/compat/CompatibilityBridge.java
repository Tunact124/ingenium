package com.ingenium.compat;

import com.ingenium.config.IngeniumConfig;
import com.ingenium.util.IngeniumLogger;

public final class CompatibilityBridge {
 
    // ── Detection flags (computed once at class load, before any Mixin runs) ───
    public static final boolean HAS_SODIUM =
        isModLoaded("sodium");
    public static final boolean HAS_LITHIUM =
        isModLoaded("lithium");
    public static final boolean HAS_FERRITECORE =
        isModLoaded("ferritecore");
    public static final boolean HAS_STARLIGHT =
        isModLoaded("starlight");
    public static final boolean HAS_BOBBY =
        isModLoaded("bobby");
    public static final boolean HAS_FABRIC_ITEM_API =
        isModLoaded("fabric-item-api-v1");
 
    // ── Version-specific guards ─────────────────────────────────────
    // Sodium 0.5.x changed its VertexConsumerProvider API.
    // Guard against using the old path if Sodium >=0.5 is present.
    public static final boolean SODIUM_NEW_API =
        HAS_SODIUM && classExists(
            "me.jellysquid.mods.sodium.client.render.chunk.DefaultChunkRenderer");
 
    public static void logSummary() {
        if (IngeniumConfig.get().verboseCompatLogging) {
            IngeniumLogger.info("=== Ingenium Compat Scan ===");
            IngeniumLogger.info("  Sodium        : " + HAS_SODIUM +
                (SODIUM_NEW_API ? " (new API)" : ""));
            IngeniumLogger.info("  Lithium       : " + HAS_LITHIUM);
            IngeniumLogger.info("  FerriteCore   : " + HAS_FERRITECORE);
            IngeniumLogger.info("  Starlight     : " + HAS_STARLIGHT);
            IngeniumLogger.info("  Bobby         : " + HAS_BOBBY);
            IngeniumLogger.info("  Fabric Item API: " + HAS_FABRIC_ITEM_API);
        }
    }
 
    private static boolean isModLoaded(String modId) {
        return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(modId);
    }

    private static boolean classExists(String fqcn) {
        try {
            Class.forName(fqcn, false,
                CompatibilityBridge.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) { return false; }
    }
}
