package com.ingenium.memory;

import com.ingenium.config.IngeniumConfig;
import net.minecraft.util.math.BlockPos;

public final class BlockPosPool {
 
    private static final ObjectPool<BlockPos.Mutable> POOL = new ObjectPool<>(
        BlockPos.Mutable::new,
        pos -> pos.set(0, 0, 0),
        IngeniumConfig.get().blockPosPoolCapacity
    );
 
    public static BlockPos.Mutable acquire(int x, int y, int z) {
        return POOL.acquire().set(x, y, z);
    }
 
    public static void release(BlockPos.Mutable pos) {
        POOL.release(pos);
    }
}
