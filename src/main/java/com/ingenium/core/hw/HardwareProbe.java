package com.ingenium.core.hw;

import org.slf4j.Logger;

/**
 * Hardware assessment:
 * - logical processors
 * - max heap
 * - deterministic single-thread calibration loop
 * - yields a stable score + tier
 */
public final class HardwareProbe {
    private HardwareProbe() {}

    public static HardwareProfile probe(Logger log) {
        int procs = Runtime.getRuntime().availableProcessors();
        long maxHeap = Runtime.getRuntime().maxMemory();

        // Deterministic CPU loop: warmup 10ms, measure ~50ms
        double opsPerMs = CalibrationLoop.measureOpsPerMs(10, 50);

        int score = computeQualityScore(procs, maxHeap, opsPerMs);
        HardwareTier tier = tierFromScore(score);

        if (log != null) {
            log.info("[Ingenium] HW probe: procs={}, maxHeapMiB={}, opsPerMs={}",
                    procs, (maxHeap / (1024L * 1024L)), String.format(java.util.Locale.ROOT, "%.1f", opsPerMs));
        }

        return new HardwareProfile(procs, maxHeap, opsPerMs, score, tier);
    }

    /**
     * Score components:
     * - CPU parallel capacity (procs)
     * - memory budget (heap)
     * - single-thread math throughput (opsPerMs)
     *
     * Keep this simple and monotonic; tiers are coarse.
     */
    private static int computeQualityScore(int procs, long maxHeapBytes, double opsPerMs) {
        int cpu = clamp(procs, 1, 32) * 10; // up to 320

        long heapMiB = maxHeapBytes / (1024L * 1024L);
        int mem = (int) clamp(heapMiB / 256L, 1, 32) * 8; // up to 256

        // opsPerMs is machine dependent; normalize roughly.
        // You can re-tune after you see real user telemetry.
        int perf = (int) clamp((long) (opsPerMs / 50.0), 1, 32) * 12; // up to 384

        return cpu + mem + perf;
    }

    private static HardwareTier tierFromScore(int score) {
        // Coarse thresholds: adjust later based on metrics from real machines
        if (score < 400) return HardwareTier.LOW;
        if (score < 700) return HardwareTier.MID;
        return HardwareTier.HIGH;
    }

    private static long clamp(long v, long lo, long hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
