package com.ingenium.world;

import com.ingenium.collections.IngeniumCollections;
import com.ingenium.collections.LongObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;

import java.util.List;
import java.util.function.Supplier;

public final class PoiQueryCache {
    private static final int CELL_SHIFT = 4; // 16-block cells
    private static final int TTL_TICKS = 10;
    private static final int EXPECTED_SIZE = 8_192;

    private final LongObjectMap<Entry> entries = IngeniumCollections.newRemovalHeavyLongObjectMap(EXPECTED_SIZE);

    public boolean isCacheable(Object poiPredicate, Object posPredicate) {
        // Lambdas are not stable across callsites; keep it strict.
        // If later you want to whitelist known predicates, add it here.
        return poiPredicate != null && posPredicate != null
            && poiPredicate.getClass().getName().startsWith("net.minecraft")
            && posPredicate.getClass().getName().startsWith("net.minecraft");
    }

    public List<BlockPos> findOrCompute(BlockPos origin, int radius, PoiManager.Occupancy occupancy, long nowTick, Supplier<List<BlockPos>> computer) {
        long key = packKey(origin, radius, occupancy);
        var existing = entries.get(key);

        if (existing != null && nowTick <= existing.validUntilTick) {
            return existing.positions;
        }

        var computed = computer.get();
        entries.put(key, new Entry(nowTick + TTL_TICKS, computed));
        return computed;
    }

    private static long packKey(BlockPos origin, int radius, PoiManager.Occupancy occupancy) {
        int cx = origin.getX() >> CELL_SHIFT;
        int cy = origin.getY() >> CELL_SHIFT;
        int cz = origin.getZ() >> CELL_SHIFT;

        long h = 1469598103934665603L;
        h = mix(h, cx);
        h = mix(h, cy);
        h = mix(h, cz);
        h = mix(h, radius);
        h = mix(h, occupancy.ordinal());
        return h;
    }

    private static long mix(long hash, int value) {
        hash ^= value * 0x9E3779B9;
        hash *= 1099511628211L;
        return hash;
    }

    private record Entry(long validUntilTick, List<BlockPos> positions) {}
}
