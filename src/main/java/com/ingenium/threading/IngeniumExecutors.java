package com.ingenium.threading;

import com.ingenium.util.IngeniumLogger;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class IngeniumExecutors {
 
    // ── IO Pool: disk-bound work (chunk save, config flush) ─────────
    // Fixed at 2 — IO is almost always the bottleneck, more threads don't help.
    public static final ExecutorService IO_POOL =
        Executors.newFixedThreadPool(2, namedDaemon("ingenium-io-%d"));
 
    // ── Compute Pool: CPU-bound work (pathfinding, region analysis) ──
    // -2 so the server and GC threads always have headroom.
    public static final ExecutorService COMPUTE_POOL =
        Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() - 2),
            namedDaemon("ingenium-compute-%d"));
 
    // ── Scheduler: periodic maintenance tasks ───────────────────────
    public static final ScheduledExecutorService SCHEDULER =
        Executors.newSingleThreadScheduledExecutor(namedDaemon("ingenium-sched-0"));
 
    // ── Commit Queue: results from async threads → server thread ────
    // ConcurrentLinkedQueue: lock-free MPSC (many producers, one consumer).
    // Each CommitTask knows its priority and target (world, entity, etc.).
    public static final Queue<CommitTask> COMMIT_QUEUE = new ConcurrentLinkedQueue<>();
 
    /**
     * Drain the commit queue on the server thread.
     * maxMs: time budget from Governor.getAiBudgetMs().
     * maxCount: hard cap to prevent a flooded queue from dominating a tick.
     */
    public static void drainCommitQueue(long maxMs, int maxCount) {
        long deadline = System.nanoTime() + maxMs * 1_000_000L;
        int count = 0;
        CommitTask task;
        while (count < maxCount && (task = COMMIT_QUEUE.poll()) != null) {
            try { task.run(); } catch (Exception e) {
                IngeniumLogger.error("Commit error in " + task.description(), e);
            }
            count++;
            if ((count & 7) == 0 && System.nanoTime() > deadline) break;
        }
    }
 
    private static ThreadFactory namedDaemon(String pattern) {
        AtomicInteger id = new AtomicInteger(0);
        return r -> { Thread t = new Thread(r,
            String.format(pattern, id.getAndIncrement()));
            t.setDaemon(true); return t; };
    }
 
    @FunctionalInterface
    public interface CommitTask {
        void run();
        default String description() { return "unknown"; }
    }
}
