$content = @'
package com.ingenium.logic;

import com.ingenium.performance.IngeniumStats;
import com.ingenium.config.IngeniumConfig;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * SpatialGrid - Optimized Entity Query System
 */
public class SpatialGrid {
    private final Long2ObjectOpenHashMap<ObjectOpenHashSet<Entity>> cells = new Long2ObjectOpenHashMap<>();
    private final Reference2LongOpenHashMap<Entity> entityToCell = new Reference2LongOpenHashMap<>();
    private int cellSize = 16;
    private int ticksSinceRebuild = 0;

    private static final ThreadLocal<ArrayList<Entity>> QUERY_BUF =
            ThreadLocal.withInitial(() -> new ArrayList<>(64));

    public SpatialGrid() {
        entityToCell.defaultReturnValue(-1L);
    }

    public void addEntityToCell(Entity e, long key) {
        cells.computeIfAbsent(key, k -> new ObjectOpenHashSet<>()).add(e);
        entityToCell.put(e, key);
    }

    public void removeEntityFromCell(Entity e, long key) {
        ObjectOpenHashSet<Entity> cell = cells.get(key);
        if (cell != null) {
            cell.remove(e);
            if (cell.isEmpty()) {
                cells.remove(key);
            }
        }
        entityToCell.removeLong(e);
    }

    public void updateEntity(Entity e, double oldX, double oldZ) {
        long oldKey = packCell(oldX, oldZ);
        long newKey = packCell(e.getX(), e.getZ());

        if (oldKey == newKey) return;

        removeEntityFromCell(e, oldKey);
        addEntityToCell(e, newKey);
    }

    private long packCell(double x, double z) {
        int cx = Math.floorDiv((int) x, cellSize);
        int cz = Math.floorDiv((int) z, cellSize);
        return ((long) (cx & 0xFFFFFFFFL)) | ((long) cz << 32);
    }

    public <T extends Entity> List<T> queryBox(Box box, Class<T> cls, Predicate<? super T> pred) {
        if (!IngeniumConfig.ENABLE_SPATIAL_GRID) return null;

        ArrayList<Entity> buf = QUERY_BUF.get();
        buf.clear();

        int minX = Math.floorDiv((int) box.minX, cellSize);
        int maxX = Math.floorDiv((int) box.maxX, cellSize);
        int minZ = Math.floorDiv((int) box.minZ, cellSize);
        int maxZ = Math.floorDiv((int) box.maxZ, cellSize);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                long key = ((long) (x & 0xFFFFFFFFL)) | ((long) z << 32);
                ObjectOpenHashSet<Entity> cell = cells.get(key);
                if (cell != null) {
                    for (Entity e : cell) {
                        if (cls.isInstance(e) && e.getBoundingBox().intersects(box) && (pred == null || pred.test(cls.cast(e)))) {
                            buf.add(e);
                        }
                    }
                }
            }
        }

        IngeniumStats.recordGridQuery();
        return (List<T>) new ArrayList<>(buf);
    }

    public void recalculateCellSize(World world) {
        int entityCount = entityToCell.size();
        if (entityCount < 10) {
            cellSize = 16;
            return;
        }

        int loadedChunks = world.getChunkManager().getLoadedChunkCount();
        double worldArea = loadedChunks * 256.0;
        int optimal = (int) Math.sqrt(worldArea / entityCount);
        int newSize = MathHelper.clamp(optimal, 4, 16);

        if (newSize != cellSize) {
            cellSize = newSize;
            rebuildGrid();
        }
    }

    private void rebuildGrid() {
        List<Entity> entities = new ArrayList<>(entityToCell.keySet());
        cells.clear();
        entityToCell.clear();
        for (Entity e : entities) {
            addEntityToCell(e, packCell(e.getX(), e.getZ()));
        }
    }

    public void onTick(World world) {
        ticksSinceRebuild++;
        if (ticksSinceRebuild >= 200) {
            recalculateCellSize(world);
            ticksSinceRebuild = 0;
        }
    }

    public int getEntityCount() {
        return entityToCell.size();
    }

    public int getCellCount() {
        return cells.size();
    }
    
    public void insert(Entity entity) {
        addEntityToCell(entity, packCell(entity.getX(), entity.getZ()));
    }
    
    public void remove(Entity entity) {
        long key = entityToCell.getLong(entity);
        if (key != -1L) {
            removeEntityFromCell(entity, key);
        }
    }

    public void update(Entity entity) {
        insert(entity);
    }
}
'@
Set-Content -Path src\main\java\com\ingenium\logic\SpatialGrid.java -Value $content -NoNewline
