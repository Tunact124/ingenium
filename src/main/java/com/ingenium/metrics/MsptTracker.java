package com.ingenium.metrics;

/**
 * Unified MSPT tracker used on both client and server.
 * Records total tick time in nanoseconds and exposes mean MSPT.
 */
public final class MsptTracker {
    private final LongRingBuffer tickNs;

    public MsptTracker(int samplesPow2) {
        this.tickNs = new LongRingBuffer(samplesPow2);
    }

    public void recordTickNs(long ns) {
        tickNs.add(ns);
    }

    public double meanMs(int maxSamples) {
        int n = Math.min(tickNs.size(), maxSamples);
        if (n <= 0) return 0.0;

        long sum = 0L;
        for (int i = 0; i < n; i++) sum += tickNs.getAtNewestOffset(i);
        return (sum / (double) n) * 1e-6;
    }

    public double newestMs() {
        return tickNs.getNewest() * 1e-6;
    }

    /**
     * Return average MSPT over the last 100 ticks (approx).
     * Used by the Governor for hysteresis decisions.
     */
    public double avgMspt100() {
        return meanMs(100);
    }
}
