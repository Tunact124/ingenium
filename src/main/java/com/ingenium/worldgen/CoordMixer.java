package com.ingenium.worldgen;

/**
 * Zero-allocation coordinate keying for caches/diagnostics.
 * This is NOT a BlockPos; it’s a stable packed key.
 */
public final class CoordMixer {
    private CoordMixer() {}

    // Pack 3 ints into one long with reversible-ish mixing.
    // You can also use this as a cache key for per-cell scratch if needed.
    public static long packKey(int x, int y, int z) {
        long lx = (x & 0x1FFFFFL); // 21 bits
        long ly = (y & 0xFFFFFL);  // 20 bits
        long lz = (z & 0x1FFFFFL); // 21 bits
        long v = (lx) | (ly << 21) | (lz << 41);

        // Final mix (xorshift-like) for better distribution if used in hash maps
        v ^= (v >>> 33);
        v *= 0xff51afd7ed558ccdL;
        v ^= (v >>> 33);
        v *= 0xc4ceb9fe1a85ec53L;
        v ^= (v >>> 33);
        return v;
    }
}
