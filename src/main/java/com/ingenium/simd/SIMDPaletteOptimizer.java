package com.ingenium.simd;

import com.ingenium.config.IngeniumConfig;

public final class SIMDPaletteOptimizer {
    private static final boolean VECTOR_AVAILABLE;

    static {
        boolean ok = false;
        if (IngeniumConfig.getInstance().simdPaletteEnabled) {
            try {
                Class.forName("jdk.incubator.vector.IntVector");
                ok = true;
            } catch (Throwable ignored) {
                ok = false;
            }
        }
        VECTOR_AVAILABLE = ok;
    }

    private SIMDPaletteOptimizer() {}

    public static boolean vectorAvailable() {
        return VECTOR_AVAILABLE;
    }

    public static int countMatching(int[] palette, int stateId) {
        // Reflection-based vector path intentionally omitted in this drop.
        // Refinement AI can add direct Vector API in a Java19+ sourceSet.
        int c = 0;
        for (int v : palette) if (v == stateId) c++;
        return c;
    }
}
