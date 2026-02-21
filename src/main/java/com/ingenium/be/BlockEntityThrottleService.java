package com.ingenium.be;

import com.ingenium.config.IngeniumConfig;
import com.ingenium.core.IngeniumGovernor;
import com.ingenium.offheap.IBeMetadataStore;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

/**
 * Glue layer between Minecraft BE ticking and the core throttle policy.
 *
 * <p>Persists per-BE counters (skip count, last tick time, flags) in the off-heap metadata store
 * to avoid long-lived heap objects.
 */
public final class BlockEntityThrottleService {

    private final IBeMetadataStore store;

    public BlockEntityThrottleService(IBeMetadataStore store) {
        this.store = store;
    }

    /**
     * Determines whether a BE should tick. Updates off-heap metadata accordingly.
     *
     * @param world server world
     * @param be block entity
     * @param pos be position
     * @return true if tick should proceed
     */
    public boolean shouldTick(ServerLevel world, BlockEntity be, BlockPos pos) {
        if (!IngeniumConfig.get().throttleBlockEntities()) return true;

        final long time = world.getGameTime();

        // Identify record by packed pos key (stable)
        final long key = pos.asLong();

        // Read state
        int skip = store.getSkipCount(key);
        int maxSkip = IngeniumConfig.get().beMaxSkipCount();

        // Distance to nearest player (cheap approximation; Architect can swap for cached nearest player dist)
        int distSq = computeNearestPlayerDistSq(world, pos);
        int criticalRadius = IngeniumConfig.get().beCriticalRadiusBlocks();
        int criticalRadiusSq = criticalRadius * criticalRadius;

        int divisor = IngeniumGovernor.get().profile().beDivisor; // expect profile API
        var decision = BlockEntityThrottlePolicy.shouldTick(new BlockEntityThrottlePolicy.Inputs(
                time, distSq, criticalRadiusSq, divisor, maxSkip, skip
        ));

        if (decision.type() == BlockEntityThrottlePolicy.DecisionType.TICK) {
            store.setSkipCount(key, 0);
            store.setLastTickTime(key, time);
            return true;
        } else {
            store.setSkipCount(key, Math.min(skip + 1, maxSkip));
            return false;
        }
    }

    public static BlockEntityThrottleService createDefault() {
        // Use factory to get the best store (off-heap or on-heap)
        int capacity = IngeniumConfig.get().offHeapBeMetadataCapacity;
        return new BlockEntityThrottleService(com.ingenium.offheap.BeMetadataStores.createDefault(capacity));
    }

    private static int computeNearestPlayerDistSq(ServerLevel world, BlockPos pos) {
        // Keep it simple & safe: iterate players; no allocations.
        int best = Integer.MAX_VALUE;
        var players = world.players();
        for (int i = 0; i < players.size(); i++) {
            var p = players.get(i);
            int dx = p.getBlockX() - pos.getX();
            int dy = p.getBlockY() - pos.getY();
            int dz = p.getBlockZ() - pos.getZ();
            int d = dx*dx + dy*dy + dz*dz;
            if (d < best) best = d;
        }
        return best;
    }
}
