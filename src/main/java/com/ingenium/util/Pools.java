package com.ingenium.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;

public final class Pools {
    // BlockPos.Mutable pool: heavily used in entity collision checks
    public static final ObjectPool<BlockPos.Mutable> BLOCK_POS
        = new ObjectPool<>(
            BlockPos.Mutable::new,
            pos -> pos.set(0, 0, 0)
        );

    // ArrayList pool for entity query results
    public static final ObjectPool<ArrayList<Entity>> ENTITY_LIST
        = new ObjectPool<>(
            () -> new ArrayList<>(16),
            ArrayList::clear
        );

    private Pools() {}
}
