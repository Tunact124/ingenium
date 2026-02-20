package com.ingenium.be;

import com.ingenium.config.IngeniumConfig;
import com.ingenium.core.IngeniumGovernor;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Server-thread only policy. No allocations in hot path.
 * - CRITICAL: within critical radius -> always tick.
 * - STANDARD: near players -> ticks with Governor divisor.
 * - LOW: far -> ticks at 2x divisor or skipped under EMERGENCY.
 */
public final class BlockEntityThrottlePolicy {

    private BlockEntityThrottlePolicy() {}

    public static boolean shouldTick(ServerWorld world, BlockEntity be, long worldTime) {
        if (!IngeniumConfig.get().blockEntityThrottlingEnabled) return true;

        IngeniumGovernor gov = IngeniumGovernor.get();
        IngeniumGovernor.OptimizationProfile p = gov.profile();

        // In AGGRESSIVE/BALANCED, mostly don't throttle.
        if (p == IngeniumGovernor.OptimizationProfile.AGGRESSIVE) return true;

        BlockPos pos = be.getPos();
        int critR = IngeniumConfig.get().blockEntityCriticalRadius;
        int critRSq = critR * critR;

        List<ServerPlayerEntity> players = world.getPlayers();
        for (int i = 0; i < players.size(); i++) {
            ServerPlayerEntity pl = players.get(i);
            double dx = (pos.getX() + 0.5) - pl.getX();
            double dy = (pos.getY() + 0.5) - pl.getY();
            double dz = (pos.getZ() + 0.5) - pl.getZ();
            if ((dx * dx + dy * dy + dz * dz) <= critRSq) {
                return true; // CRITICAL
            }
        }

        int divisor = gov.blockEntityTickDivisor();
        boolean allow = (worldTime % divisor) == 0;

        if (!allow) return false;

        // Budget gate (coarse default cost; refinement AI can calibrate with timing)
        if (!gov.consumeBudget(IngeniumGovernor.Subsystem.BLOCK_ENTITIES, 50_000L)) {
            return false;
        }

        // EMERGENCY can be stricter:
        if (p == IngeniumGovernor.OptimizationProfile.EMERGENCY) {
            // deterministic 50% sampling to spread load
            int h = mixPos(pos);
            return (h & 1) == 0;
        }

        return true;
    }

    private static int mixPos(BlockPos p) {
        int x = p.getX(), y = p.getY(), z = p.getZ();
        int h = x * 73428767 ^ y * 912931 ^ z * 423233;
        h ^= (h >>> 16);
        h *= 0x7feb352d;
        h ^= (h >>> 15);
        h *= 0x846ca68b;
        h ^= (h >>> 16);
        return h;
    }
}
