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
     * Takes a vanilla entity list and returns batches grouped by section.
     */
    public List<List<Entity>> batchBySpatialLocality(List<Entity> entities) {
        // Reset reusable state
        sectionBuckets.clear();
        sortedBatches.clear();
        poolIndex = 0;
        
        // Phase 1: Bucket entities by chunk section
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            long sectionKey = SectionPos.asLong(
                SectionPos.blockToSectionCoord((int) entity.getX()),
                SectionPos.blockToSectionCoord((int) entity.getY()),
                SectionPos.blockToSectionCoord((int) entity.getZ())
            );
            
            List<Entity> bucket = sectionBuckets.get(sectionKey);
            if (bucket == null) {
                bucket = acquireList();
                sectionBuckets.put(sectionKey, bucket);
            }
            bucket.add(entity);
        }
        
        // Phase 2: Collect batches
        for (List<Entity> batch : sectionBuckets.values()) {
            if (!batch.isEmpty()) {
                sortedBatches.add(batch);
            }
        }
        
        return sortedBatches;
    }
    
    private List<Entity> acquireList() {
        if (poolIndex < listPool.size()) {
            List<Entity> list = listPool.get(poolIndex++);
            list.clear();
            return list;
        }
        List<Entity> newList = new ArrayList<>(16);
        listPool.add(newList);
        poolIndex++;
        return newList;
    }
}
