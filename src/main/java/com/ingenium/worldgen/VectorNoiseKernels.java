package com.ingenium.worldgen;

import com.ingenium.core.VectorGuard;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * SIMD kernels used by worldgen mixins.
 * All methods are allocation-free and rely on ThreadLocal scratch from caller.
 */
public final class VectorNoiseKernels {
    private static final VectorSpecies<Float> FS = FloatVector.SPECIES_PREFERRED;

    private VectorNoiseKernels() {}

    /**
     * Vectorized lerp: out[i] = a[i] + t[i] * (b[i] - a[i])
     * Uses FMA: a + t*(b-a).
     *
     * Preconditions:
     * - arrays length >= laneCount
     */
    public static void lerpFma(float[] a, float[] b, float[] t, float[] out, int laneCount) {
        // Runtime guard: if Vector API is unavailable/disabled, scalar fallback.
        if (!VectorGuard.isAvailable() || laneCount <= 1) {
            for (int i = 0; i < laneCount; i++) {
                out[i] = Math.fma(t[i], (b[i] - a[i]), a[i]);
            }
            return;
        }

        // Use only as many lanes as we staged (4 or 8 typically).
        // We still load using SPECIES_PREFERRED, but mask if needed.
        var species = FS;
        if (laneCount != species.length()) {
            // Masked load/store path
            var m = species.indexInRange(0, laneCount);
            FloatVector va = FloatVector.fromArray(species, a, 0, m);
            FloatVector vb = FloatVector.fromArray(species, b, 0, m);
            FloatVector vt = FloatVector.fromArray(species, t, 0, m);

            // a + t*(b-a)
            FloatVector vout = vb.sub(va).fma(vt, va);
            vout.intoArray(out, 0, m);
            return;
        }

        FloatVector va = FloatVector.fromArray(species, a, 0);
        FloatVector vb = FloatVector.fromArray(species, b, 0);
        FloatVector vt = FloatVector.fromArray(species, t, 0);

        FloatVector vout = vb.sub(va).fma(vt, va);
        vout.intoArray(out, 0);
    }

    /**
     * “Superfast square” helper: x^2 via mul (no pow).
     */
    public static float sq(float x) {
        return x * x;
    }
}
