package com.ingenium.hash;

/**
 * Fibonacci/phi-based integer mixing for hash quality improvement.
 * Ported from EfficientHashing (ZZZank, LGPL-3.0).
 */
public final class PhiMixHash {

    public static final int PHI = 0x9E3779B9;

    public static int mix(int x) {
        final int y = x * PHI;
        return y ^ (y >>> 16);
    }

    public static int hashCoordinates(int x, int y, int z) {
        return mix(mix(x) + y) + z;
    }

    private PhiMixHash() {}
}
