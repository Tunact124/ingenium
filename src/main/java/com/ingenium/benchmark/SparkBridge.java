package com.ingenium.benchmark;

import com.ingenium.config.IngeniumConfig;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Soft integration point for Spark (profiling mod).
 *
 * <p>This class avoids linking against Spark APIs directly. The Architect can optionally implement
 * reflection-based adapters if desired.
 */
public final class SparkBridge {

    private static final String SPARK_MODID = "spark";

    private SparkBridge() {}

    /**
     * @return true if Spark appears to be loaded and integration is enabled.
     */
    public static boolean enabled() {
        return IngeniumConfig.get().sparkIntegrationEnabled()
                && FabricLoader.getInstance().isModLoaded(SPARK_MODID);
    }

    /**
     * Hook point: let Architect implement reflection calls into Spark if desired.
     */
    public static void markSection(String name) {
        if (!enabled()) return;
        // Architect: If you want, add reflection calls into Spark sampler/section API here.
    }
}
