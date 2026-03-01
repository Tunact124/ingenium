package com.ingenium.entity;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity Spatial Batcher
 * 
 * Groups entities by their chunk section before ticking.
 * This ensures that entities near each other in the world are
 * ticked sequentially, maximizing CPU cache locality.
 */
public final class EntitySpatialBatcher {

    // Reusable per-tick storage — avoids allocation
    private final Long2ObjectOpenHashMap<List<Entity>> sectionBuckets;
    private final List<List<Entity>> sortedBatches;

    // Pool of entity lists to avoid GC churn
    private final List<List<Entity>> listPool;
    private int poolIndex = 0;

    public EntitySpatialBatcher() {
        this.sectionBuckets = new Long2ObjectOpenHashMap<>(256);
        this.sortedBatches = new ArrayList<>(256);
        this.listPool = new ArrayList<>(256);

        // Pre-allocate pool
        for (int i = 0; i < 256; i++) {
            listPool.add(new ArrayList<>(16));
        }
    }

    /**
     * Executes entity ticks directly from the tick list but applies
     * minimal spatial sorting to maintain cache locality without overhead.
     */
    public void processSpatially(net.minecraft.world.level.entity.EntityTickList instance,
            java.util.function.Consumer<Entity> action) {
        // Clear previous state without allocating
        for (int i = 0; i < poolIndex; i++) {
            listPool.get(i).clear();
        }
        sectionBuckets.clear();
        poolIndex = 0;

        // Phase 1: Bucket entities directly from the tick list stream
        instance.forEach(entity -> {
            long sectionKey = SectionPos.asLong(
                    SectionPos.blockToSectionCoord((int) entity.getX()),
                    SectionPos.blockToSectionCoord((int) entity.getY()),
                    SectionPos.blockToSectionCoord((int) entity.getZ()));

            List<Entity> bucket = sectionBuckets.get(sectionKey);
            if (bucket == null) {
                bucket = acquireList();
                sectionBuckets.put(sectionKey, bucket);
            }
            bucket.add(entity);
        });

        // Phase 2: Execute action directly on the buckets
        for (List<Entity> batch : sectionBuckets.values()) {
            // Hot loop - no iterator allocation
            for (int i = 0; i < batch.size(); i++) {
                action.accept(batch.get(i));
            }
        }
    }

    private List<Entity> acquireList() {
        if (poolIndex < listPool.size()) {
            return listPool.get(poolIndex++); // Already cleared
        }
        List<Entity> newList = new ArrayList<>(16);
        listPool.add(newList);
        poolIndex++;
        return newList;
    }
}
