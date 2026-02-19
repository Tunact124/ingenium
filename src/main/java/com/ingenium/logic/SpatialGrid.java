package com.ingenium.logic;

import com.ingenium.config.IngeniumConfig;
import com.ingenium.util.IngeniumLogger;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * SpatialGrid - Optimized Entity Query System
 */
public class SpatialGrid {
    private final Long2ObjectOpenHashMap<ObjectArrayList<Entity>> grid = new Long2ObjectOpenHashMap<>(4096);
    private final Reference2LongOpenHashMap<Entity> entityToCell = new Reference2LongOpenHashMap<>();
    private int cellSize = 4;
    private long lastRecalcTick = 0;
    private static final long RECALC_INTERVAL = 200;  // Every 10 seconds

    // Optional context (used by tests and some adaptive behaviors)
    private World world;
    private IngeniumConfig config;

    // Expose min/max for tests
    public static final int MIN_CELL_SIZE = 4;
    public static final int MAX_CELL_SIZE = 16;

    private static final ThreadLocal<ArrayList<Entity>> QUERY_BUF =
            ThreadLocal.withInitial(() -> new ArrayList<>(64));

    public SpatialGrid() {
        entityToCell.defaultReturnValue(-1L);
    }

    public SpatialGrid(World world, IngeniumConfig config) {
        this();
        this.world = world;
        this.config = config;
    }

    // ZERO ALLOCATION: manual get/put, no lambda, no computeIfAbsent
    public void addEntity(Entity entity) {
        long key = packCell(entity.getX(), entity.getZ());
        ObjectArrayList<Entity> cell = grid.get(key);
        if (cell == null) {
            cell = new ObjectArrayList<>(8);  // FastUtil, not ArrayList
            grid.put(key, cell);
        }
        cell.add(entity);
        entityToCell.put(entity, key);
    }

    public void removeEntity(Entity entity) {
        long key = entityToCell.getLong(entity);
        if (key != -1L) {
            ObjectArrayList<Entity> cell = grid.get(key);
            if (cell != null) {
                cell.remove(entity);
                if (cell.isEmpty()) {
                    grid.remove(key);  // Free immediately, do not keep empty cells
                }
            }
            entityToCell.removeLong(entity);
        }
    }

    public void addEntityToCell(Entity e, long key) {
        ObjectArrayList<Entity> cell = grid.get(key);
        if (cell == null) {
            cell = new ObjectArrayList<>(8);
            grid.put(key, cell);
        }
        cell.add(e);
        entityToCell.put(e, key);
    }

    public void removeEntityFromCell(Entity e, long key) {
        ObjectArrayList<Entity> cell = grid.get(key);
        if (cell != null) {
            cell.remove(e);
            if (cell.isEmpty()) {
                grid.remove(key);
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

    public List<Entity> queryBox(Box box) {
        return (List<Entity>) queryBox(box, Entity.class, null);
    }

    public <T extends Entity> List<T> queryBox(Box box, Class<T> cls, Predicate<? super T> pred) {
        ArrayList<Entity> buf = QUERY_BUF.get();
        buf.clear();

        int minX = Math.floorDiv((int) box.minX, cellSize);
        int maxX = Math.floorDiv((int) box.maxX, cellSize);
        int minZ = Math.floorDiv((int) box.minZ, cellSize);
        int maxZ = Math.floorDiv((int) box.maxZ, cellSize);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                long key = ((long) (x & 0xFFFFFFFFL)) | ((long) z << 32);
                ObjectArrayList<Entity> cell = grid.get(key);
                if (cell != null) {
                    for (Entity e : cell) {
                        if (cls.isInstance(e) && e.getBoundingBox().intersects(box) && (pred == null || pred.test(cls.cast(e)))) {
                            buf.add(e);
                        }
                    }
                }
            }
        }

        return (List<T>) new ArrayList<>(buf);
    }

    public List<Entity> queryRadius(Vec3d center, double radius) {
        Box box = new Box(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius
        );
        // Filter by exact distance to avoid including corner entities
        List<Entity> inBox = queryBox(box);
        if (inBox == null) return null;
        ArrayList<Entity> precise = new ArrayList<>(inBox.size());
        for (Entity e : inBox) {
            if (e.getPos().distanceTo(center) <= radius) precise.add(e);
        }
        return precise;
    }

    public void maybeRecalcCellSize(int entityCount, int loadedChunks, long worldTime) {
        if (worldTime - lastRecalcTick < RECALC_INTERVAL) return;
        lastRecalcTick = worldTime;

        if (entityCount < 10) {
            setCellSize(16);  // Very sparse: large cells, fast iteration
            return;
        }

        double worldArea = (double) loadedChunks * 256.0;  // 16x16 per chunk
        int optimal = (int) Math.sqrt(worldArea / entityCount);
        int clamped = Math.max(4, Math.min(16, optimal));

        if (clamped != cellSize) {
            setCellSize(clamped);
            IngeniumLogger.debug(String.format("[SpatialGrid] Cell size: %d (entities=%d, chunks=%d)",
                clamped, entityCount, loadedChunks));
        }
    }

    private void setCellSize(int newSize) {
        if (newSize == this.cellSize) return;
        this.cellSize = newSize;
        // Rebuild: move all entities into correct new cells
        List<Entity> all = new ArrayList<>(entityToCell.keySet());
        grid.clear();
        entityToCell.clear();
        for (Entity e : all) {
            addEntityToCell(e, packCell(e.getX(), e.getZ()));
        }
    }

    public void onTick(World world) {
        maybeRecalcCellSize(entityToCell.size(), world.getChunkManager().getLoadedChunkCount(), world.getTime());
    }

    public int getEntityCount() {
        return entityToCell.size();
    }

    public int getCellCount() {
        return grid.size();
    }

    public int getCellSize() {
        return cellSize;
    }

    public void clear() {
        grid.clear();
        entityToCell.clear();
    }
    
    public void insert(Entity entity) {
        if (entity == null) throw new NullPointerException("entity");
        addEntityToCell(entity, packCell(entity.getX(), entity.getZ()));
    }
    
    public void remove(Entity entity) {
        long key = entityToCell.getLong(entity);
        if (key != -1L) {
            removeEntityFromCell(entity, key);
        }
    }

    public void update(Entity entity) {
        if (entity == null) return;
        long oldKey = entityToCell.getLong(entity);
        long newKey = packCell(entity.getX(), entity.getZ());
        if (oldKey == -1L) {
            addEntityToCell(entity, newKey);
            return;
        }
        if (oldKey != newKey) {
            removeEntityFromCell(entity, oldKey);
            addEntityToCell(entity, newKey);
        }
    }
}