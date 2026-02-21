package com.ingenium.metrics;

/**
 * Stores per-phase ring buffers and provides cheap aggregates.
 * Zero-allocation:
 * - fixed ring buffers
 * - percentile uses a scratch array (thread-confined) you can allocate once
 *   outside hot paths (or compute approximate p95/p99 differently later).
 */
public final class PhaseTimings {
    private final LongRingBuffer[] rings;
    private final boolean[] enabled;

    public PhaseTimings(int samplesPow2) {
        Phase[] phases = Phase.values();
        this.rings = new LongRingBuffer[phases.length];
        this.enabled = new boolean[phases.length];

        for (int i = 0; i < phases.length; i++) {
            rings[i] = new LongRingBuffer(samplesPow2);
            enabled[i] = false;
        }
    }

    public void setEnabled(Phase phase, boolean on) {
        enabled[phase.ordinal()] = on;
    }

    public boolean isEnabled(Phase phase) {
        return enabled[phase.ordinal()];
    }

    public void recordNs(Phase phase, long durationNs) {
        if (!enabled[phase.ordinal()]) return;
        rings[phase.ordinal()].add(durationNs);
    }

    public long newestNs(Phase phase) {
        return rings[phase.ordinal()].getNewest();
    }

    public double newestMs(Phase phase) {
        return newestNs(phase) * 1e-6;
    }

    public double meanMs(Phase phase, int maxSamples) {
        LongRingBuffer rb = rings[phase.ordinal()];
        int n = Math.min(rb.size(), maxSamples);
        if (n <= 0) return 0.0;

        long sum = 0L;
        for (int i = 0; i < n; i++) {
            sum += rb.getAtNewestOffset(i);
        }
        return (sum / (double) n) * 1e-6;
    }
}
