package com.ingenium.tick;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cancellation handle for wheel-scheduled entries.
 *
 * <p>Key property: cancellation is done via an MPSC queue; we do NOT remove from bucket lists on cancel.
 * The wheel drains and simply skips entries whose handle is CANCELLED.
 */
public final class ScheduledTickHandle {
    /** INIT -> (CANCELLED xor EXPIRED). */
    private final AtomicInteger state = new AtomicInteger(0);

    public static final int INIT = 0;
    public static final int CANCELLED = 1;
    public static final int EXPIRED = 2;

    /**
     * Attempts to cancel this handle.
     * @return true if we transitioned INIT -> CANCELLED; false if already cancelled/expired.
     */
    public boolean cancel() {
        return state.compareAndSet(INIT, CANCELLED);
    }

    /**
     * Marks this handle expired (executed).
     * @return true if we transitioned INIT -> EXPIRED; false if already cancelled/expired.
     */
    public boolean expire() {
        return state.compareAndSet(INIT, EXPIRED);
    }

    /** @return true if cancelled. */
    public boolean isCancelled() {
        return state.get() == CANCELLED;
    }

    /** @return true if expired. */
    public boolean isExpired() {
        return state.get() == EXPIRED;
    }
}
