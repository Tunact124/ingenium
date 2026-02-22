package com.ingenium.poi;

import com.ingenium.ds.LongObjHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Spatial hash cache for Point of Interest queries.
 */
public final class POISpatialCache {

    private static final Logger LOG = LoggerFactory.getLogger("Ingenium/POICache");

    private static final int CELL_SHIFT = 5;
    private static final int COORD_BITS = 21;

    private final LongObjHashMap<List<POIEntry>> cells;
    private long generation = 0;
    private final java.util.Set<Long> dirtyCells = new java.util.HashSet<>();

    public POISpatialCache() {
        this.cells = new LongObjHashMap<>(256);
    }

    public static long cellKey(int x, int y, int z) {
        long cx = ((long) (x >> CELL_SHIFT)) & ((1L << COORD_BITS) - 1);
        long cy = ((long) (y >> CELL_SHIFT)) & ((1L << COORD_BITS) - 1);
        long cz = ((long) (z >> CELL_SHIFT)) & ((1L << COORD_BITS) - 1);
        return (cx << (COORD_BITS * 2)) | (cy << COORD_BITS) | cz;
    }

    public void addPOI(int x, int y, int z, Object poiType, Object poiRecord) {
        long key = cellKey(x, y, z);
        List<POIEntry> cell = cells.get(key);
        if (cell == null) {
            cell = new ArrayList<>(4);
            cells.put(key, cell);
        }
        cell.add(new POIEntry(x, y, z, poiType, poiRecord));
        dirtyCells.add(key);
    }

    public void removePOI(int x, int y, int z) {
        long key = cellKey(x, y, z);
        List<POIEntry> cell = cells.get(key);
        if (cell != null) {
            cell.removeIf(e -> e.x == x && e.y == y && e.z == z);
            if (cell.isEmpty()) {
                cells.remove(key);
            }
            dirtyCells.add(key);
        }
    }

    public List<POIEntry> getInRange(int centerX, int centerY, int centerZ,
                                      int radius, Predicate<Object> typePredicate) {

        List<POIEntry> results = new ArrayList<>();
        long radiusSq = (long) radius * radius;

        int minCX = (centerX - radius) >> CELL_SHIFT;
        int maxCX = (centerX + radius) >> CELL_SHIFT;
        int minCY = (centerY - radius) >> CELL_SHIFT;
        int maxCY = (centerY + radius) >> CELL_SHIFT;
        int minCZ = (centerZ - radius) >> CELL_SHIFT;
        int maxCZ = (centerZ + radius) >> CELL_SHIFT;

        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cy = minCY; cy <= maxCY; cy++) {
                for (int cz = minCZ; cz <= maxCZ; cz++) {
                    long key = cellKey(cx << CELL_SHIFT, cy << CELL_SHIFT, cz << CELL_SHIFT);
                    List<POIEntry> cell = cells.get(key);
                    if (cell == null) continue;

                    for (POIEntry entry : cell) {
                        long dx = entry.x - centerX;
                        long dy = entry.y - centerY;
                        long dz = entry.z - centerZ;
                        long distSq = dx * dx + dy * dy + dz * dz;

                        if (distSq <= radiusSq && typePredicate.test(entry.poiType)) {
                            results.add(entry);
                        }
                    }
                }
            }
        }
        return results;
    }

    public void tick() {
        generation++;
        dirtyCells.clear();
    }

    public void invalidateChunk(int chunkX, int chunkZ) {
        int minCX = (chunkX << 4) >> CELL_SHIFT;
        int maxCX = ((chunkX << 4) + 15) >> CELL_SHIFT;
        int minCZ = (chunkZ << 4) >> CELL_SHIFT;
        int maxCZ = ((chunkZ << 4) + 15) >> CELL_SHIFT;

        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                for (int cy = -2; cy <= 10; cy++) {
                    long key = cellKey(cx << CELL_SHIFT, cy << CELL_SHIFT, cz << CELL_SHIFT);
                    cells.remove(key);
                }
            }
        }
    }

    public long getGeneration() {
        return generation;
    }

    public static final class POIEntry {
        public final int x, y, z;
        public final Object poiType;
        public final Object poiRecord;

        POIEntry(int x, int y, int z, Object poiType, Object poiRecord) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.poiType = poiType;
            this.poiRecord = poiRecord;
        }
    }
}
