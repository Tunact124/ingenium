package com.ingenium.core.hw;

/**
 * Deterministic calibration:
 * - runs a fixed math loop
 * - avoids allocations
 * - avoids calling into anything that might lock/allocate
 *
 * We measure "ops" as loop iterations with a few fused operations.
 * Result: ops per millisecond.
 */
public final class CalibrationLoop {
    private CalibrationLoop() {}

    public static double measureOpsPerMs(int warmupMs, int measureMs) {
        // Warmup
        runForMillis(warmupMs);

        // Measure
        long start = System.nanoTime();
        long end = start + (long) measureMs * 1_000_000L;

        long ops = 0L;
        long x = 0x9E3779B97F4A7C15L; // fixed seed
        long y = 0xC2B2AE3D27D4EB4FL;

        while (System.nanoTime() < end) {
            // A small mix of shifts/xors/muls: deterministic and CPU-bound.
            x ^= x << 13;
            x ^= x >>> 7;
            x ^= x << 17;

            y += 0x9E3779B97F4A7C15L;
            x = (x * 0xBF58476D1CE4E5B9L) ^ y;

            ops++;
        }

        long elapsedNs = System.nanoTime() - start;
        double elapsedMs = elapsedNs / 1_000_000.0;
        if (elapsedMs <= 0.0) return 0.0;
        return ops / elapsedMs;
    }

    private static void runForMillis(int ms) {
        long end = System.nanoTime() + (long) ms * 1_000_000L;

        long x = 0xD1B54A32D192ED03L;
        while (System.nanoTime() < end) {
            // lightweight warm loop
            x ^= x << 7;
            x ^= x >>> 9;
        }

        // Prevent JIT from proving loop unused (very minor safeguard).
        if (x == 42) {
            throw new AssertionError();
        }
    }
}
