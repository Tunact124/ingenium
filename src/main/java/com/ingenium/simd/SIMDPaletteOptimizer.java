package com.ingenium.simd;

import com.ingenium.config.IngeniumConfig;

/**
 * Facade for palette scan operations. Internally delegates to the selected implementation.
 *
 * <p>Keep callsites stable even if you add a true Vector implementation later.
 */
public final class SIMDPaletteOptimizer {
    private static volatile IPaletteOptimizer impl = PaletteOptimizerFactory.create();

    private SIMDPaletteOptimizer() {}

    /**
     * Re-evaluates the implementation selection (e.g., after config reload).
     */
    public static void reload() {
        impl = PaletteOptimizerFactory.create();
    }

    public static int countEquals(int[] data, int needle) {
        return impl.countEquals(data, needle);
    }

    public static int countNonZero(int[] data) {
        return impl.countNonZero(data);
    }

    public static int countMatching(int[] palette, int stateId) {
        return countEquals(palette, stateId);
    }

    public static boolean vectorAvailable() {
        return impl.getClass().getSimpleName().contains("Vector");
    }
}
