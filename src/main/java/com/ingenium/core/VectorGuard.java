package com.ingenium.core;

import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.IntVector;
import org.slf4j.Logger;

/**
 * Centralized guard for Vector API usage.
 *
 * Notes:
 * - On Java 21, Vector API is still incubating. You must run with:
 *   --add-modules jdk.incubator.vector
 * - This guard lets you safely fall back to scalar math if:
 *   - module isn't present
 *   - JVM disables vectorization
 *   - an unexpected runtime exception occurs
 */
public final class VectorGuard {
    private static volatile boolean available;
    private static volatile int preferredIntLanes;

    private VectorGuard() {}

    public static void init(Logger log) {
        boolean ok = false;
        int lanes = 1;

        try {
            // Touch a Vector class to confirm module presence & basic functionality.
            VectorSpecies<Integer> species = IntVector.SPECIES_PREFERRED;
            lanes = species.length();

            // Very small sanity check (no allocations)
            var v = IntVector.zero(species);
            ok = (v.length() == lanes && lanes > 0);
        } catch (Throwable t) {
            ok = false;
            lanes = 1;
            if (log != null) {
                log.warn("[Ingenium] Vector API disabled/fallback: {}", t.toString());
            }
        }

        available = ok;
        preferredIntLanes = lanes;

        if (log != null) {
            log.info("[Ingenium] Vector API: available={}, preferredIntLanes={}", available, preferredIntLanes);
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static int preferredIntLanes() {
        return preferredIntLanes;
    }
}
