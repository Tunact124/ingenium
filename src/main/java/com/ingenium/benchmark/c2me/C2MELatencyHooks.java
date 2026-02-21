package com.ingenium.benchmark.c2me;

import com.ingenium.benchmark.IngeniumBenchmarkService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Best-effort C2ME readiness hook.
 *
 * Strategy:
 * - locate a known completion point in C2ME pipeline via reflection
 * - attach a callback that calls ChunkLatencyMonitor.recordReady(...)
 *
 * This code must NEVER crash the server if C2ME changes internals.
 */
public final class C2MELatencyHooks {

    private static volatile boolean installed = false;

    public static void tryInstall() {
        if (installed) return;
        installed = true;

        try {
            // Example: class names below are placeholders; you must adjust to the actual C2ME classes in your target.
            // The point is: do it reflectively and fail-safe.
            Class<?> clz = Class.forName("com.ishland.c2me.base.common.threading.worldgen.ChunkTaskScheduler");
            // If found, you’d hook a completion listener. Many C2ME builds don’t expose stable listeners,
            // so you may need a mixin into C2ME itself (still optional via plugin gating).
        } catch (Throwable ignored) {
            // No-op: fall back to vanilla hooks
        }
    }

    /**
     * Called by a C2ME mixin (optional) or by a reflective callback if you find one.
     */
    public static void onChunkTrulyReady(ServerLevel world, int chunkX, int chunkZ) {
        long now = System.nanoTime();
        long packed = ChunkPos.asLong(chunkX, chunkZ);
        IngeniumBenchmarkService.get().getChunkLatency().recordReady(world.dimension(), packed, now);
    }

    private C2MELatencyHooks() {}
}
