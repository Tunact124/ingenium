package com.ingenium.core;

import com.ingenium.config.IngeniumConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central executor wiring for Ingenium.
 *
 * <p>Mandates:
 * <ul>
 *   <li>Java 17 compatibility.</li>
 *   <li>Optional virtual-thread IO pool when running on Java 21+ (reflection only).</li>
 *   <li>Bounded main-thread commit queue (Netty-style backpressure).</li>
 * </ul>
 */
public final class IngeniumExecutors {
    private static final Logger LOGGER = LogManager.getLogger("Ingenium/Executors");

    private static volatile ForkJoinPool COMPUTE;
    private static volatile ExecutorService IO;

    // Backpressure: bounded queue. Tasks are executed on the server thread via processCommitQueue().
    private static final int DEFAULT_COMMIT_CAP = 2048;
    private static final AtomicInteger COMPUTE_ID = new AtomicInteger(1);
    private static final AtomicInteger IO_ID = new AtomicInteger(1);

    private static volatile ArrayBlockingQueue<Runnable> COMMIT_QUEUE =
            new ArrayBlockingQueue<>(DEFAULT_COMMIT_CAP);

    private static volatile boolean started;
    private static volatile boolean shutdown;

    private IngeniumExecutors() {}

    /** Ensure pools exist; safe to call multiple times. */
    public static synchronized void ensureStarted() {
        if (started) return;
        if (shutdown) return;

        final IngeniumConfig cfg = IngeniumConfig.get();
        final int commitCap = (cfg != null) ? Math.max(256, cfg.core().commitQueueCapacity()) : DEFAULT_COMMIT_CAP;
        COMMIT_QUEUE = new ArrayBlockingQueue<>(commitCap);

        COMPUTE = createComputePool(cfg);
        IO = createIoPool(cfg);

        started = true;
        LOGGER.info("Executors started. commitCap={}, computeParallelism={}, io={}",
                commitCap,
                COMPUTE.getParallelism(),
                IO.getClass().getSimpleName());
    }

    public static ForkJoinPool compute() {
        ensureStarted();
        return COMPUTE;
    }

    public static ExecutorService io() {
        ensureStarted();
        return IO;
    }

    /**
     * Enqueue a main-thread commit task. Non-blocking; drops tasks if saturated.
     *
     * <p>This is intentionally lossy under overload (safety-first): it prevents worker threads
     * from blocking and cascading into deadlocks.
     */
    public static void submitCommit(Runnable task) {
        Objects.requireNonNull(task, "task");
        if (shutdown) return;
        ensureStarted();

        if (!COMMIT_QUEUE.offer(task)) {
            // Avoid spam: caller should aggregate if this happens often.
            LOGGER.warn("Commit queue full ({}). Dropping task.", COMMIT_QUEUE.size());
        }
    }

    /**
     * Run up to {@code maxTasks} commits on the server thread.
     *
     * @return number processed
     */
    public static int processCommitQueue(int maxTasks) {
        if (!started || shutdown) return 0;
        int processed = 0;

        while (processed < maxTasks) {
            final Runnable r = COMMIT_QUEUE.poll();
            if (r == null) break;
            try {
                r.run();
            } catch (Throwable t) {
                LOGGER.error("Commit task crashed (continuing).", t);
            }
            processed++;
        }
        return processed;
    }

    public static synchronized void shutdown() {
        if (shutdown) return;
        shutdown = true;

        if (COMPUTE != null) COMPUTE.shutdown();
        if (IO != null) IO.shutdown();

        COMMIT_QUEUE.clear();
        LOGGER.info("Executors shutdown.");
    }

    private static ForkJoinPool createComputePool(IngeniumConfig cfg) {
        final int cores = Runtime.getRuntime().availableProcessors();
        final double mult = (cfg != null) ? cfg.core().computePoolMultiplier() : 1.0;

        // Keep 1 core for the server thread; never below 2.
        final int parallelism = Math.max(2, (int) Math.floor(Math.max(1, cores - 1) * mult));

        return new ForkJoinPool(
                parallelism,
                pool -> {
                    ForkJoinWorkerThread t = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                    t.setName("Ingenium-Compute-" + COMPUTE_ID.getAndIncrement());
                    t.setPriority(Thread.NORM_PRIORITY - 1);
                    t.setDaemon(true);
                    return t;
                },
                (t, e) -> LOGGER.error("Compute pool uncaught exception", e),
                true
        );
    }

    private static ExecutorService createIoPool(IngeniumConfig cfg) {
        final boolean preferVirtual = (cfg == null) || cfg.core().useVirtualThreadsForIO();
        final ThreadFactory virtualFactory = preferVirtual ? tryVirtualThreadFactory("Ingenium-IO-VT-") : null;

        if (virtualFactory != null) {
            LOGGER.info("IO pool using virtual threads (Java 21+ detected).");
            // Java 21+: Executors.newThreadPerTaskExecutor(factory)
            // Use reflection to avoid linking errors on Java 17.
            final ExecutorService vt = tryNewThreadPerTaskExecutor(virtualFactory);
            if (vt != null) return vt;
        }

        // Java 17 fallback: cached-style executor suitable for bursty IO.
        final int core = (cfg != null) ? Math.max(1, cfg.core().ioCoreThreads()) : 4;
        return new ThreadPoolExecutor(
                core,
                Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                r -> {
                    Thread t = new Thread(r, "Ingenium-IO-" + IO_ID.getAndIncrement());
                    t.setPriority(Thread.NORM_PRIORITY);
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * Java 21+ only: Thread.ofVirtual().name(prefix, 0).factory()
     * Returns null on Java 17.
     */
    private static ThreadFactory tryVirtualThreadFactory(String prefix) {
        try {
            Method ofVirtual = Thread.class.getMethod("ofVirtual");
            Object builder = ofVirtual.invoke(null);

            // Thread.Builder is a nested type; reflect its methods.
            Class<?> builderClass = Class.forName("java.lang.Thread$Builder");
            Method name = builderClass.getMethod("name", String.class, long.class);
            Method factory = builderClass.getMethod("factory");

            Object named = name.invoke(builder, prefix, 0L);
            return (ThreadFactory) factory.invoke(named);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Java 21+: Executors.newThreadPerTaskExecutor(ThreadFactory)
     */
    private static ExecutorService tryNewThreadPerTaskExecutor(ThreadFactory factory) {
        try {
            Method m = Executors.class.getMethod("newThreadPerTaskExecutor", ThreadFactory.class);
            return (ExecutorService) m.invoke(null, factory);
        } catch (Throwable ignored) {
            return null;
        }
    }

    // Legacy/Compatibility methods
    public static void init() { ensureStarted(); }
    public static void submitCompute(Runnable task) { compute().execute(task); }
    public static void submitIO(Runnable task) { io().execute(task); }
    public static int drainCommitQueue(long budgetNs) { return processCommitQueue(100); }
    public static int commitQueueSize() { return COMMIT_QUEUE.size(); }
}
