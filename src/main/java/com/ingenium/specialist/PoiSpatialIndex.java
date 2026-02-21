package com.ingenium.specialist;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Contract: This index is per-thread/per-tick. It is not safe to share across threads.
 * It exists to collapse repeated POI lookups into O(1) section bucket probes.
 */
public final class PoiSpatialIndex {
    private final Long2ObjectOpenHashMap<List<BlockEntity>> buckets = new Long2ObjectOpenHashMap<>();
    private long builtForTick = Long.MIN_VALUE;

    public void beginTick(long gameTick) {
        if (builtForTick == gameTick) return;
        builtForTick = gameTick;
        buckets.clear();
    }

    public void add(BlockEntity poiLikeEntity) {
        var sectionKey = SectionPos.asLong(
                SectionPos.blockToSectionCoord(poiLikeEntity.getBlockPos().getX()),
                SectionPos.blockToSectionCoord(poiLikeEntity.getBlockPos().getY()),
                SectionPos.blockToSectionCoord(poiLikeEntity.getBlockPos().getZ())
        );

        var list = buckets.get(sectionKey);
        if (list == null) {
            list = new ArrayList<>(8);
            buckets.put(sectionKey, list);
        }
        list.add(poiLikeEntity);
    }

    public List<BlockEntity> getSectionBucket(int sectionX, int sectionY, int sectionZ) {
        return buckets.get(SectionPos.asLong(sectionX, sectionY, sectionZ));
    }
}
