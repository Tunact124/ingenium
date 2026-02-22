package com.ingenium.cache;

import com.ingenium.core.IngeniumGovernor;
import com.ingenium.core.IngeniumGovernor.OptimizationProfile;
import com.ingenium.offheap.OffHeapBlockEntityStore;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Ingenium's cache state across dimension transitions.
 * 
 * Instead of destroying all caches on dimension change, we park them
 * in a dimension-keyed registry. When the player returns to a dimension,
 * we restore the warm caches instead of cold-starting.
 */
public final class DimensionCacheRegistry {
    
    private static final Logger LOG = LoggerFactory.getLogger("Ingenium/DimCache");
    
    // Maximum number of dimensions to keep cached simultaneously
    private static final int MAX_CACHED_DIMENSIONS = 3;
    
    private final ConcurrentHashMap<ResourceKey<Level>, DimensionCacheSnapshot> snapshots;
    private final EvictionOrder evictionOrder;
    
    public DimensionCacheRegistry() {
        this.snapshots = new ConcurrentHashMap<>(4);
        this.evictionOrder = new EvictionOrder(MAX_CACHED_DIMENSIONS);
    }
    
    /**
     * Called when a dimension is about to be unloaded for a player.
     * Takes a snapshot of all Ingenium caches for this dimension.
     */
    public void parkDimension(
            ResourceKey<Level> dimension,
            IngeniumGovernor governor,
            OffHeapBlockEntityStore beStore) {
        
        LOG.debug("[Ingenium] Parking caches for dimension: {}", dimension.location());
        
        // Evict oldest if at capacity
        if (snapshots.size() >= MAX_CACHED_DIMENSIONS) {
            ResourceKey<Level> evicted = evictionOrder.evictOldest();
            if (evicted != null) {
                DimensionCacheSnapshot old = snapshots.remove(evicted);
                if (old != null) {
                    old.release(); // Free off-heap memory
                    LOG.debug("[Ingenium] Evicted cache for dimension: {}", evicted.location());
                }
            }
        }
        
        DimensionCacheSnapshot snapshot = new DimensionCacheSnapshot(
            governor.profile(),
            governor.currentMspt(),
            0, // profileStabilityTicks not currently tracked as a field
            beStore.createSnapshot()
        );
        
        snapshots.put(dimension, snapshot);
        evictionOrder.touch(dimension);
        
        LOG.info("[Ingenium] Parked {} caches for {}", 
                 snapshot.beSnapshotEntryCount(), dimension.location());
    }
    
    /**
     * Called when a dimension is being loaded/entered.
     * Restores cached state if available, otherwise returns null.
     */
    public DimensionCacheSnapshot restoreDimension(ResourceKey<Level> dimension) {
        DimensionCacheSnapshot snapshot = snapshots.get(dimension);
        
        if (snapshot == null) {
            LOG.debug("[Ingenium] No cached state for dimension: {} — cold start", dimension.location());
            return null;
        }
        
        // Validate snapshot isn't too old — 10 minutes
        if (snapshot.ageInTicks() > 20 * 60 * 10) { 
            LOG.debug("[Ingenium] Cached state for {} too old — discarding", dimension.location());
            snapshots.remove(dimension);
            snapshot.release();
            return null;
        }
        
        evictionOrder.touch(dimension);
        LOG.info("[Ingenium] Restored warm caches for {} (profile: {}, {} BE entries)",
                 dimension.location(), 
                 snapshot.governorProfile(),
                 snapshot.beSnapshotEntryCount());
        
        return snapshot;
    }
    
    public void releaseAll() {
        for (DimensionCacheSnapshot snapshot : snapshots.values()) {
            snapshot.release();
        }
        snapshots.clear();
        evictionOrder.clear();
        LOG.info("[Ingenium] Released all dimension cache snapshots");
    }
    
    public static final class DimensionCacheSnapshot {
        private final OptimizationProfile governorProfile;
        private final double recentMsptAverage;
        private final long profileStabilityTicks;
        private final OffHeapBlockEntityStore.Snapshot beSnapshot;
        private final long createdAtNano;
        private boolean released = false;
        
        public DimensionCacheSnapshot(
                OptimizationProfile profile,
                double msptAvg,
                long stabilityTicks,
                OffHeapBlockEntityStore.Snapshot beSnapshot) {
            this.governorProfile = profile;
            this.recentMsptAverage = msptAvg;
            this.profileStabilityTicks = stabilityTicks;
            this.beSnapshot = beSnapshot;
            this.createdAtNano = System.nanoTime();
        }
        
        public OptimizationProfile governorProfile() { return governorProfile; }
        public double recentMsptAverage() { return recentMsptAverage; }
        public long profileStabilityTicks() { return profileStabilityTicks; }
        public OffHeapBlockEntityStore.Snapshot beSnapshot() { return beSnapshot; }
        
        public int beSnapshotEntryCount() {
            return beSnapshot != null ? beSnapshot.entryCount() : 0;
        }
        
        public long ageInTicks() {
            long elapsedNano = System.nanoTime() - createdAtNano;
            return elapsedNano / 50_000_000L; 
        }
        
        public void release() {
            if (!released && beSnapshot != null) {
                beSnapshot.free();
                released = true;
            }
        }
    }
    
    private static final class EvictionOrder {
        private final int maxSize;
        private final java.util.LinkedHashMap<ResourceKey<Level>, Long> accessOrder;
        
        EvictionOrder(int maxSize) {
            this.maxSize = maxSize;
            this.accessOrder = new java.util.LinkedHashMap<>(maxSize + 1, 0.75f, true);
        }
        
        void touch(ResourceKey<Level> key) {
            accessOrder.put(key, System.nanoTime());
        }
        
        ResourceKey<Level> evictOldest() {
            var iterator = accessOrder.entrySet().iterator();
            if (iterator.hasNext()) {
                var entry = iterator.next();
                iterator.remove();
                return entry.getKey();
            }
            return null;
        }
        
        void clear() {
            accessOrder.clear();
        }
    }
}
