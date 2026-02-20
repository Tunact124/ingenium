package com.ingenium.simd;

/**
 * Palette / int-array scan utilities.
 *
 * <p>Java 17 production-safe: implementations must not require incubator modules.
 */
public interface IPaletteOptimizer {

    /**
     * Counts how many entries in {@code data} equal {@code needle}.
     */
    int countEquals(int[] data, int needle);

    /**
     * Counts how many entries in {@code data} are non-zero.
     */
    int countNonZero(int[] data);
}
