package com.ingenium.benchmark;

import com.ingenium.core.IngeniumGovernor;

import java.util.EnumMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Central metrics aggregation for Ingenium.
 *
 * <p>Design:
 * <ul>
 *   <li>LongAdder for high-contention counters.</li>
 *   <li>AtomicLong for "last" values and low-frequency snapshots.</li>
 *   <li>Stores rolling tick costs per subsystem for diagnostics and governor feedback.</li>
 * </ul>
 */
public final class IngeniumDiagnostics {

    private static final IngeniumDiagnostics INSTANCE = new IngeniumDiagnostics();

    public static IngeniumDiagnostics get() {
        return INSTANCE;
    }

    /** How many ticks of history to keep for rolling averages (power-of-two recommended). */
    public static final int HISTORY = 256;

    private final EnumMap<IngeniumGovernor.SubsystemType, RollingNs> rollingBySubsystem =
            new EnumMap<>(IngeniumGovernor.SubsystemType.class);

    private final LongAdder ticksObserved = new LongAdder();
    private final AtomicLong lastTickStartNs = new AtomicLong(0L);
    private final AtomicLong lastTickEndNs = new AtomicLong(0L);

    /** "System time share": how much wall time we spent inside tracked subsystems vs total tick wall time. */
    private final AtomicLong lastSystemSharePermille = new AtomicLong(0L);

    private IngeniumDiagnostics() {
        for (IngeniumGovernor.SubsystemType s : IngeniumGovernor.SubsystemType.values()) {
            rollingBySubsystem.put(s, new RollingNs(HISTORY));
        }
    }

    /**
     * Mark the beginning of a server tick (or world tick) window.
     */
    public void onTickStart(long nowNs) {
        lastTickStartNs.set(nowNs);
    }

    /**
     * Mark the end of a server tick (or world tick) window and compute time share.
     */
    public void onTickEnd(long nowNs) {
        lastTickEndNs.set(nowNs);
        ticksObserved.increment();

        final long tickWallNs = Math.max(1L, nowNs - lastTickStartNs.get());
        long trackedNs = 0L;
        for (RollingNs r : rollingBySubsystem.values()) {
            trackedNs += r.lastValueNs();
        }

        // permille (0..1000) to avoid floats in hot paths
        long permille = Math.min(1000L, (trackedNs * 1000L) / tickWallNs);
        lastSystemSharePermille.set(permille);
    }

    /**
     * Record a subsystem cost for the current tick.
     *
     * <p>Call sites should use try-with-resources from {@link SubsystemTimer}.
     */
    public void recordSubsystemCost(IngeniumGovernor.SubsystemType subsystem, long costNs) {
        rollingBySubsystem.get(subsystem).push(costNs);
    }

    /** @return last computed system share in permille (parts per 1000). */
    public long lastSystemSharePermille() {
        return lastSystemSharePermille.get();
    }

    /** @return rolling average cost for subsystem in microseconds. */
    public long avgMicros(IngeniumGovernor.SubsystemType subsystem) {
        return TimeUnit.NANOSECONDS.toMicros(rollingBySubsystem.get(subsystem).avgNs());
    }

    /** @return last tick's cost for subsystem in microseconds. */
    public long lastMicros(IngeniumGovernor.SubsystemType subsystem) {
        return TimeUnit.NANOSECONDS.toMicros(rollingBySubsystem.get(subsystem).lastValueNs());
    }

    /** @return ticks observed since boot. */
    public long ticksObserved() {
        return ticksObserved.sum();
    }

    /**
     * Small fixed-size rolling window for long values.
     */
    public static final class RollingNs {
        private final long[] ring;
        private final int mask; // ring length must be power of 2
        private int idx;
        private long sum;
        private long last;

        public RollingNs(int size) {
            int cap = 1;
            while (cap < size) cap <<= 1;
            this.ring = new long[cap];
            this.mask = cap - 1;
        }

        /**
         * Push a new value into the rolling window.
         */
        public void push(long value) {
            int i = idx++ & mask;
            long prev = ring[i];
            ring[i] = value;
            sum += (value - prev);
            last = value;
        }

        public long avgNs() {
            return sum / ring.length;
        }

        public long lastValueNs() {
            return last;
        }
    }

    /**
     * Convenience timer that reports to {@link IngeniumDiagnostics}.
     *
     * <p>Usage:
     * <pre>
     * try (var t = diagnostics.timer(SubsystemType.BLOCK_ENTITY)) {
     *   ...
     * }
     * </pre>
     */
    public SubsystemTimer timer(IngeniumGovernor.SubsystemType subsystem) {
        return new SubsystemTimer(this, subsystem, System.nanoTime());
    }

    /**
     * AutoCloseable timer used by try-with-resources.
     */
    public static final class SubsystemTimer implements AutoCloseable {
        private final IngeniumDiagnostics diag;
        private final IngeniumGovernor.SubsystemType subsystem;
        private final long startNs;
        private boolean closed;

        private SubsystemTimer(IngeniumDiagnostics diag, IngeniumGovernor.SubsystemType subsystem, long startNs) {
            this.diag = diag;
            this.subsystem = subsystem;
            this.startNs = startNs;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            long cost = System.nanoTime() - startNs;
            diag.recordSubsystemCost(subsystem, Math.max(0L, cost));
        }
    }

    // Legacy/Compatibility methods for external code
    public void onServerStartThreadCaptured() {}
    public void onChunkMainThreadWait(long durationNs) {}

    public String governorSummary() { return "governor=unavailable"; }
    public String asyncQueueSummary() { return "asyncQueue=unavailable"; }
    public String wheelSummary() { return "wheel=unavailable"; }
    public double lastTickMs() { return 0.0; }
    public long allocBytesDeltaWindow() { return 0L; }
    public long gcTimeDeltaWindowMs() { return 0L; }
    public long gcCountDeltaWindow() { return 0L; }
    public long chunkRequestCount() { return 0L; }
    public long chunkReadyCount() { return 0L; }
    public double chunkLatencyAvgMs() { return 0.0; }
    public double chunkLatencyMaxMs() { return 0.0; }
    public double chunkMainThreadWaitAvgMs() { return 0.0; }
    public double chunkMainThreadWaitMaxMs() { return 0.0; }
}
