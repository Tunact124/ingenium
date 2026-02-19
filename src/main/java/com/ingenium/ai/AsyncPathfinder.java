package com.ingenium.ai;

import com.ingenium.config.IngeniumConfig;
import com.ingenium.governor.IngeniumGovernor;
import com.ingenium.threading.ChunkStampRegistry;
import com.ingenium.threading.IngeniumExecutors;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public final class AsyncPathfinder {
 
    /**
     * Entry point: schedule a pathfinding task for a villager.
     * Called from a @Inject mixin into VillagerEntity.tick().
     */
    public static void schedulePathfind(
            VillagerEntity villager,
            BlockPos       target,
            IngeniumGovernor governor) {
 
        if (!IngeniumConfig.get().enableAsyncPathfinding) return;
 
        ServerWorld world   = (ServerWorld) villager.getWorld();
        ChunkPos chunkPos   = new ChunkPos(villager.getBlockPos());
 
        // Snapshot version at scheduling time
        long versionAtSchedule = ChunkStampRegistry.getVersion(chunkPos);
 
        // PathContext is an immutable value object — safe to pass to async thread.
        // It holds a snapshot of block states needed for A* traversal.
        PathContext ctx = PathContext.snapshot(
            world, villager.getBlockPos(), target,
            /* radius= */ 16
        );
 
        IngeniumExecutors.COMPUTE_POOL.submit(() -> {
            // ── COMPUTE PHASE (async thread) ───────────────────────
            Path path = LightweightAStar.compute(ctx);
            if (path == null) return;
 
            // ── COMMIT PHASE (back to server thread) ──────────────
            long taskDescription = versionAtSchedule; // captured for lambda
            IngeniumExecutors.COMMIT_QUEUE.offer(new IngeniumExecutors.CommitTask() {
                @Override public void run() {
                    // Stale check: did the chunk change while we computed?
                    long currentVersion = ChunkStampRegistry.getVersion(chunkPos);
                    if (currentVersion != taskDescription) return; // discard
                    if (!villager.isAlive()) return; // entity removed
                    villager.getNavigation().startMovingAlong(path, 1.0);
                }
                @Override public String description() {
                    return "villager-path@" + villager.getUuidAsString();
                }
            });
        });
    }
}
