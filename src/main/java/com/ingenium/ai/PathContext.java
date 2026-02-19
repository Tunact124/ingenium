package com.ingenium.ai;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.HashMap;
import java.util.Map;

public record PathContext(Map<BlockPos, BlockState> blockSnapshot, BlockPos start, BlockPos target) {
    public static PathContext snapshot(ServerWorld world, BlockPos start, BlockPos target, int radius) {
        Map<BlockPos, BlockState> snapshot = new HashMap<>();
        for (BlockPos pos : BlockPos.iterate(start.add(-radius, -radius, -radius), start.add(radius, radius, radius))) {
            snapshot.put(pos.toImmutable(), world.getBlockState(pos));
        }
        return new PathContext(snapshot, start.toImmutable(), target.toImmutable());
    }
}
