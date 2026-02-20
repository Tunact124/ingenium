package com.ingenium.simd;

import com.ingenium.config.IngeniumConfig;

/**
 * Selects the best available palette optimizer.
 *
 * <p>Java 17-safe: defaults to scalar. Optionally attempts Vector API if enabled and present.
 */
public final class PaletteOptimizerFactory {
    private PaletteOptimizerFactory() {}

    public static IPaletteOptimizer create() {
        if (!IngeniumConfig.get().simdPaletteEnabled) {
            return new ScalarPaletteOptimizer();
        }

        // Attempt optional Vector API implementation (not provided here to avoid hard dependency).
        // This is a hook point: you can ship an additional jar/module later.
        try {
            Class<?> cls = Class.forName("com.ingenium.simd.VectorPaletteOptimizer");
            return (IPaletteOptimizer) cls.getConstructor().newInstance();
        } catch (Throwable ignored) {
            return new ScalarPaletteOptimizer();
        }
    }
}
