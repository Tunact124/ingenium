package com.ingenium.metrics;

import net.fabricmc.api.EnvType;

/**
 * Central metrics registry.
 * - Owns PhaseTimings (per-phase) and MsptTracker (unified)
 * - Provides a lightweight "span" timer API you can use from mixins
 */
public final class IngeniumMetrics {
    private final PhaseTimings timings;
    private final MsptTracker mspt;

    public IngeniumMetrics(int samplesPow2, EnvType env) {
        this.timings = new PhaseTimings(samplesPow2);
        this.mspt = new MsptTracker(samplesPow2);
        MetricsProfile.configureForEnv(env, timings);
    }

    public PhaseTimings timings() {
        return timings;
    }

    public MsptTracker mspt() {
        return mspt;
    }

    /** Hot-path span: caller stores start time in a local long. */
    public static long beginNs() {
        return System.nanoTime();
    }

    /** Hot-path end: no allocations; just arithmetic + ring add. */
    public void endRecordNs(Phase phase, long startNs) {
        timings.recordNs(phase, System.nanoTime() - startNs);
    }
}
