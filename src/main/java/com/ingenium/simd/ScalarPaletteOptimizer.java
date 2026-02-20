package com.ingenium.simd;

/**
 * Scalar, allocation-free palette scanning.
 *
 * <p>Hot-path safe for Java 17 with no special JVM flags.
 */
public final class ScalarPaletteOptimizer implements IPaletteOptimizer {

    @Override
    public int countEquals(int[] data, int needle) {
        // Unrolled loop to reduce branch overhead.
        int i = 0;
        int n = data.length;
        int count = 0;

        int limit = n & ~7;
        while (i < limit) {
            count += (data[i] == needle) ? 1 : 0;
            count += (data[i + 1] == needle) ? 1 : 0;
            count += (data[i + 2] == needle) ? 1 : 0;
            count += (data[i + 3] == needle) ? 1 : 0;
            count += (data[i + 4] == needle) ? 1 : 0;
            count += (data[i + 5] == needle) ? 1 : 0;
            count += (data[i + 6] == needle) ? 1 : 0;
            count += (data[i + 7] == needle) ? 1 : 0;
            i += 8;
        }
        while (i < n) {
            count += (data[i] == needle) ? 1 : 0;
            i++;
        }
        return count;
    }

    @Override
    public int countNonZero(int[] data) {
        int i = 0;
        int n = data.length;
        int count = 0;

        int limit = n & ~7;
        while (i < limit) {
            count += (data[i] != 0) ? 1 : 0;
            count += (data[i + 1] != 0) ? 1 : 0;
            count += (data[i + 2] != 0) ? 1 : 0;
            count += (data[i + 3] != 0) ? 1 : 0;
            count += (data[i + 4] != 0) ? 1 : 0;
            count += (data[i + 5] != 0) ? 1 : 0;
            count += (data[i + 6] != 0) ? 1 : 0;
            count += (data[i + 7] != 0) ? 1 : 0;
            i += 8;
        }
        while (i < n) {
            count += (data[i] != 0) ? 1 : 0;
            i++;
        }
        return count;
    }
}
