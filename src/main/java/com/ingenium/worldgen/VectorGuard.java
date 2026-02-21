package com.ingenium.worldgen;

public final class VectorGuard {
    private VectorGuard() {}

    public static final boolean SIMD_AVAILABLE;
    public static final int DOUBLE_LANES;

    static {
        boolean ok = false;
        int lanes = 1;

        try {
            // Touch Vector API types in a single guarded place
            lanes = jdk.incubator.vector.DoubleVector.SPECIES_PREFERRED.length();
            ok = lanes >= 2; 
        } catch (Throwable t) {
            ok = false;
            lanes = 1;
        }

        SIMD_AVAILABLE = ok;
        DOUBLE_LANES = lanes;
    }

    public static boolean isVectorAvailable() {
        return SIMD_AVAILABLE;
    }
}
