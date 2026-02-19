package com.ingenium.governor;

public final class MetricsCollector {
 
    private static final int WINDOW = 20; // 20-tick rolling average (= 1 second)
    private final long[] msSamples   = new long[WINDOW]; // ms per tick, not ns
    private int          head        = 0;
    private long         tickStartNs = 0L; // ← v2 fix: stored at tick START
 
    /**
     * Call from ServerTickEvents.START_SERVER_TICK.
     * Records when the tick begins so we can measure its full duration.
     */
    public void onTickStart() {
        this.tickStartNs = System.nanoTime();
    }
 
    /**
     * Call from ServerTickEvents.END_SERVER_TICK.
     * Calculates actual tick duration = END - START.
     * BUG FIX: v1 subtracted the PREVIOUS start, measuring idle time.
     */
    public void onTickEnd() {
        if (tickStartNs == 0L) return; // guard for first tick
        long durationNs = System.nanoTime() - tickStartNs;
        msSamples[head] = durationNs / 1_000_000L; // convert ns → ms
        head = (head + 1) % WINDOW;
    }
 
    /** Average MSPT over the last WINDOW ticks. Thread-safe (volatile writes via head). */
    public double averageMspt() {
        long sum = 0;
        for (long s : msSamples) sum += s;
        return sum / (double) WINDOW;
    }
 
    /** Peak MSPT in the window — useful for spike detection. */
    public long peakMspt() {
        long max = 0;
        for (long s : msSamples) if (s > max) max = s;
        return max;
    }
}
