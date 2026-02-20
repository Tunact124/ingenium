package com.ingenium.tick.mpsc;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Factory for cancellation queues.
 */
public final class CancelQueues {
    private CancelQueues() {}

    /**
     * Creates an MPSC cancellation queue.
     *
     * <p>If JCTools is on the classpath, prefer it; otherwise use CLQ.
     */
    public static <E> CancelQueue<E> create() {
        // Try JCTools reflectively to avoid hard dependency.
        try {
            // org.jctools.queues.MpscUnboundedArrayQueue<E>(int capacity)
            Class<?> cls = Class.forName("org.jctools.queues.MpscUnboundedArrayQueue");
            @SuppressWarnings("unchecked")
            Object q = cls.getConstructor(int.class).newInstance(1024);
            return new CancelQueue<>() {
                @Override public void offer(E e) {
                    try {
                        cls.getMethod("offer", Object.class).invoke(q, e);
                    } catch (ReflectiveOperationException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                @Override public E poll() {
                    try {
                        @SuppressWarnings("unchecked")
                        E v = (E) cls.getMethod("poll").invoke(q);
                        return v;
                    } catch (ReflectiveOperationException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        } catch (Throwable ignored) {
            // Fallback
        }

        final Queue<E> clq = new ConcurrentLinkedQueue<>();
        return new CancelQueue<>() {
            @Override public void offer(E e) { clq.offer(e); }
            @Override public E poll() { return clq.poll(); }
        };
    }
}
