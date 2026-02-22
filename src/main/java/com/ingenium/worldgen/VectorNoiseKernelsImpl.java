package com.ingenium.worldgen;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * SIMD implementation for worldgen kernels.
 * ONLY loaded if SIMDCapability.isAvailable() is true.
 */
final class VectorNoiseKernelsImpl {
    private static final VectorSpecies<Float> FS = FloatVector.SPECIES_PREFERRED;

    static void lerpFma(float[] a, float[] b, float[] t, float[] out, int laneCount) {
        var species = FS;
        if (laneCount != species.length()) {
            var m = species.indexInRange(0, laneCount);
            FloatVector va = FloatVector.fromArray(species, a, 0, m);
            FloatVector vb = FloatVector.fromArray(species, b, 0, m);
            FloatVector vt = FloatVector.fromArray(species, t, 0, m);

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
}
