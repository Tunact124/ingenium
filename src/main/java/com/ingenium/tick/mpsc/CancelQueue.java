package com.ingenium.tick.mpsc;

/**
 * Minimal MPSC cancellation queue abstraction.
 *
 * <p>We keep it tiny so we can:
 * - use JCTools MpscUnboundedArrayQueue when present (best),
 * - or fall back to ConcurrentLinkedQueue when not.
 */
public interface CancelQueue<E> {
    /** Enqueue from any thread. Must be lock-free-ish / low contention. */
    void offer(E e);

    /** Poll from the single consumer (server thread). */
    E poll();
}
