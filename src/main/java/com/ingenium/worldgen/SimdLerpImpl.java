package com.ingenium.worldgen;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * SIMD implementation for noise interpolation.
 * ONLY loaded if SIMDCapability.isAvailable() is true.
 */
final class SimdLerpImpl {
    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
    private static final ThreadLocal<double[]> SCRATCH = ThreadLocal.withInitial(() -> new double[12]);

    static void lerp4Fma(double delta,
                         double s0, double e0,
                         double s1, double e1,
                         double s2, double e2,
                         double s3, double e3,
                         SimdLerp.Out4 out) {

        final double[] a = SCRATCH.get();
        a[0] = s0; a[1] = s1; a[2] = s2; a[3] = s3;
        a[4] = e0; a[5] = e1; a[6] = e2; a[7] = e3;

        final DoubleVector vStart = DoubleVector.fromArray(SPECIES, a, 0);
        final DoubleVector vEnd   = DoubleVector.fromArray(SPECIES, a, 4);

        final DoubleVector vDiff = vEnd.sub(vStart);
        final DoubleVector vDelta = DoubleVector.broadcast(SPECIES, delta);

        final DoubleVector vOut = vDelta.lanewise(VectorOperators.FMA, vDiff, vStart);

        vOut.intoArray(a, 8);

        out.o0 = a[8];
        out.o1 = a[9];
        out.o2 = a[10];
        out.o3 = a[11];
    }
}
