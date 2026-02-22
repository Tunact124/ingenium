package com.ingenium.simd;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.DoubleVector;

/**
 * ONLY accessed if SIMDCapability.isAvailable() is true.
 * Separated to avoid ClassNotFoundException on Java 17 without incubator module.
 */
final class VectorHelper {
    static int getIntLanes() {
        return IntVector.SPECIES_PREFERRED.length();
    }

    static int getDoubleLanes() {
        return DoubleVector.SPECIES_PREFERRED.length();
    }
}
