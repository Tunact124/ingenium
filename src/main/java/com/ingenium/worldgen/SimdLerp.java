package com.ingenium.worldgen;

import com.ingenium.simd.SIMDCapability;

public final class SimdLerp {
    private SimdLerp() {}

    /** Scalar FMA lerp: start + delta * (end - start). */
    public static double lerpFmaScalar(double delta, double start, double end) {
        return Math.fma(delta, end - start, start);
    }

    /**
     * SIMD lerp for exactly 4 pairs (start[i], end[i]) with shared delta.
     * Safely falls back to scalar if SIMD is not available.
     */
    public static void lerp4Fma(double delta,
                               double s0, double e0,
                               double s1, double e1,
                               double s2, double e2,
                               double s3, double e3,
                               Out4 out) {

        if (SIMDCapability.isAvailable()) {
            SimdLerpImpl.lerp4Fma(delta, s0, e0, s1, e1, s2, e2, s3, e3, out);
        } else {
            out.o0 = lerpFmaScalar(delta, s0, e0);
            out.o1 = lerpFmaScalar(delta, s1, e1);
            out.o2 = lerpFmaScalar(delta, s2, e2);
            out.o3 = lerpFmaScalar(delta, s3, e3);
        }
    }

    public static final class Out4 {
        public double o0, o1, o2, o3;
    }
}
