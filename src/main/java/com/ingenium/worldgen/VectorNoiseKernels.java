package com.ingenium.worldgen;

import com.ingenium.simd.SIMDCapability;

/**
 * Kernels used by worldgen mixins.
 * Safely delegates to SIMD implementation if available.
 */
public final class VectorNoiseKernels {

    private VectorNoiseKernels() {}

    /**
     * Vectorized lerp: out[i] = a[i] + t[i] * (b[i] - a[i])
     * Uses FMA: a + t*(b-a).
     */
    public static void lerpFma(float[] a, float[] b, float[] t, float[] out, int laneCount) {
        if (SIMDCapability.isAvailable() && laneCount > 1) {
            VectorNoiseKernelsImpl.lerpFma(a, b, t, out, laneCount);
        } else {
            for (int i = 0; i < laneCount; i++) {
                out[i] = Math.fma(t[i], (b[i] - a[i]), a[i]);
            }
        }
    }

    /**
     * “Superfast square” helper: x^2 via mul (no pow).
     */
    public static float sq(float x) {
        return x * x;
    }
}
