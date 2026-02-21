package com.ingenium.worldgen;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

public final class SimdLerp {
    private SimdLerp() {}

    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;

    private static final ThreadLocal<double[]> SCRATCH = ThreadLocal.withInitial(() -> new double[12]);

    /** Scalar FMA lerp: start + delta * (end - start). */
    public static double lerpFmaScalar(double delta, double start, double end) {
        return Math.fma(delta, end - start, start);
    }

    /**
     * SIMD lerp for exactly 4 pairs (start[i], end[i]) with shared delta.
     */
    public static void lerp4Fma(double delta,
                               double s0, double e0,
                               double s1, double e1,
                               double s2, double e2,
                               double s3, double e3,
                               Out4 out) {

        if (!VectorGuard.SIMD_AVAILABLE) {
            out.o0 = lerpFmaScalar(delta, s0, e0);
            out.o1 = lerpFmaScalar(delta, s1, e1);
            out.o2 = lerpFmaScalar(delta, s2, e2);
            out.o3 = lerpFmaScalar(delta, s3, e3);
            return;
        }

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

    public static final class Out4 {
        public double o0, o1, o2, o3;
    }
}
